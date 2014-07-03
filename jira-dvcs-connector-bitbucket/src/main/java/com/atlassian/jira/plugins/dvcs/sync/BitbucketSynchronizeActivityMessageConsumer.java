package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryCommitMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Participant;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.PullRequestService;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientBuilder;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientBuilderFactory;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketLink;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestActivityInfo;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestApprovalActivity;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestBaseActivity;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestCommit;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestHead;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestPage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestParticipant;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestRepository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestReviewer;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestUpdateActivity;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.PullRequestRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.BitbucketSynchronizeActivityMessage;
import com.atlassian.jira.plugins.dvcs.util.ActiveObjectsUtils;
import org.apache.commons.lang.StringUtils;
import org.jfree.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;

/**
 * Consumer of {@link BitbucketSynchronizeActivityMessage}-s.
 *
 * @author Stanislav Dvorscak
 *
 */
public class BitbucketSynchronizeActivityMessageConsumer implements MessageConsumer<BitbucketSynchronizeActivityMessage>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BitbucketSynchronizeActivityMessageConsumer.class);
    private static final String ID = BitbucketSynchronizeActivityMessageConsumer.class.getCanonicalName();
    public static final String KEY = BitbucketSynchronizeActivityMessage.class.getCanonicalName();
    private static final String REVIEWER_ROLE = "REVIEWER";

    @Resource
    private MessagingService messagingService;
    @Resource
    private BitbucketClientBuilderFactory bitbucketClientBuilderFactory;
    @Resource
    private RepositoryPullRequestDao dao;
    @Resource
    private PullRequestService pullRequestService;
    @Resource
    private RepositoryDao repositoryDao;

    public BitbucketSynchronizeActivityMessageConsumer()
    {
        super();
    }

    @Override
    public void onReceive(Message<BitbucketSynchronizeActivityMessage> message, final BitbucketSynchronizeActivityMessage payload)
    {
        final Repository repo = payload.getRepository();
        final Progress progress = payload.getProgress();
        int jiraCount = progress.getJiraCount();

        BitbucketPullRequestPage<BitbucketPullRequestActivityInfo> activityPage = null;

        Date lastSync = repo.getActivityLastSync();

        BitbucketClientBuilder bitbucketClientBuilder = bitbucketClientBuilderFactory.forRepository(repo);
        PullRequestRemoteRestpoint pullRestpoint = bitbucketClientBuilder.apiVersion(2).build().getPullRequestAndCommentsRemoteRestpoint();

        // if this is the first page, use cached client
        final PullRequestRemoteRestpoint pullRestpointForActivities =
                payload.getPageNum() == 1 ?
                        bitbucketClientBuilderFactory.forRepository(repo).apiVersion(2).cached().build().getPullRequestAndCommentsRemoteRestpoint() : pullRestpoint;
        activityPage = FlightTimeInterceptor.execute(progress, new FlightTimeInterceptor.Callable<BitbucketPullRequestPage<BitbucketPullRequestActivityInfo>>()
        {
            @Override
            public BitbucketPullRequestPage<BitbucketPullRequestActivityInfo> call() throws RuntimeException
            {
                return pullRestpointForActivities.getRepositoryActivityPage(payload.getPageNum(), repo.getOrgName(), repo.getSlug(),
                        payload.getLastSyncDate());
            }
        });

        List<BitbucketPullRequestActivityInfo> infos = activityPage.getValues();
        boolean isLastPage = isLastPage(activityPage);

        for (BitbucketPullRequestActivityInfo info : infos)
        {
            Date activityDate = ClientUtils.extractActivityDate(info.getActivity());

            if (activityDate == null)
            {
                Log.info("Date for the activity could not be found.");
                continue;
            }
            if (lastSync == null || activityDate.after(lastSync))
            {
                lastSync = activityDate;
                repositoryDao.setLastActivitySyncDate(repo.getId(), activityDate);
            }

            int prIssueKeysCount = 0;
            try
            {
                int localPrId = processActivity(payload, info, pullRestpoint);
                prIssueKeysCount = dao.updatePullRequestIssueKeys(repo, localPrId);
            }
            catch (IllegalStateException e)
            {
                // This should not happen
                LOGGER.warn("Pull request " + info.getPullRequest().getId() + " from repository with " + repo.getId() + " could not be processed", e);
            }

            markProcessed(payload, info);

            progress.inPullRequestProgress(processedSize(payload),
                    jiraCount + prIssueKeysCount);
        }
        if (!isLastPage)
        {
            fireNextPage(message, payload, activityPage.getNext(), payload.getLastSyncDate());
        }

    }

    protected int processedSize(BitbucketSynchronizeActivityMessage payload)
    {
        return payload.getProcessedPullRequests() == null ? 0 : payload.getProcessedPullRequests().size();
    }

    protected void markProcessed(BitbucketSynchronizeActivityMessage payload, BitbucketPullRequestActivityInfo info)
    {
        payload.getProcessedPullRequests().add(info.getPullRequest().getId().intValue());
    }

    private void fireNextPage(Message<BitbucketSynchronizeActivityMessage> message, BitbucketSynchronizeActivityMessage payload, String nextUrl, Date lastSync)
    {
        messagingService.publish(getAddress(), new BitbucketSynchronizeActivityMessage(payload.getRepository(), null, payload.isSoftSync(),
                payload.getPageNum() + 1, payload.getProcessedPullRequests(), payload.getProcessedPullRequestsLocal(), lastSync, payload.getSyncAuditId()), getPriority(payload), message.getTags());
    }

    private int getPriority(BitbucketSynchronizeActivityMessage payload)
    {
        if (payload == null)
        {
            return MessagingService.DEFAULT_PRIORITY;
        }
        return payload.isSoftSync() ? MessagingService.SOFTSYNC_PRIORITY: MessagingService.DEFAULT_PRIORITY;
    }

    private boolean isLastPage(BitbucketPullRequestPage<BitbucketPullRequestActivityInfo> activityPage)
    {
        return activityPage.getValues().isEmpty() || activityPage.getValues().size() < PullRequestRemoteRestpoint.REPO_ACTIVITY_PAGESIZE || StringUtils.isEmpty(activityPage.getNext());
    }

    private int processActivity(BitbucketSynchronizeActivityMessage payload, BitbucketPullRequestActivityInfo info,
            PullRequestRemoteRestpoint pullRestpoint)
    {
        Repository repo = payload.getRepository();

        RepositoryPullRequestMapping localPullRequest = ensurePullRequestPresent(repo, pullRestpoint, info, payload);

        if (isUpdateActivity(info.getActivity()) && !payload.getProcessedPullRequestsLocal().contains(localPullRequest.getID()))
        {
            loadPullRequestCommits(repo, pullRestpoint, localPullRequest.getID(), (BitbucketPullRequestUpdateActivity) info.getActivity(),
                     localPullRequest, info.getPullRequest());
            payload.getProcessedPullRequestsLocal().add(localPullRequest.getID());
        }
        return localPullRequest.getID();
    }

    private RepositoryPullRequestMapping ensurePullRequestPresent(final Repository repo, final PullRequestRemoteRestpoint pullRestpoint, final BitbucketPullRequestActivityInfo info, BitbucketSynchronizeActivityMessage payload)
    {
        BitbucketPullRequest remote = null;

        Map<String, Participant> participantIndex = null;
        Integer remoteId = info.getPullRequest().getId().intValue();
        int commentCount = 0;

        // have a detail within this synchronization ?
        if (!payload.getProcessedPullRequests().contains(remoteId))
        {
            final Progress sync = repo.getSync();

            remote = FlightTimeInterceptor.execute(sync, new FlightTimeInterceptor.Callable<BitbucketPullRequest>()
            {
                @Override
                public BitbucketPullRequest call()
                {
                    BitbucketLink pullRequestLink = info.getPullRequest().getLinks().getSelf();
                    if (pullRequestLink != null && !StringUtils.isBlank(pullRequestLink.getHref()))
                    {
                        return pullRestpoint.getPullRequestDetail(pullRequestLink.getHref());
                    } else
                    {
                        // if there is no pull request link fall back to the generated pull request url
                        return pullRestpoint.getPullRequestDetail(repo.getOrgName(), repo.getSlug(), info
                                .getPullRequest().getId() + "");
                    }
                }
            });

            participantIndex = loadPulRequestParticipants(pullRestpoint, remote);

            if (remote.getLinks().getComments() != null)
            {
                final String commentsUrl = remote.getLinks().getComments().getHref();
                commentCount = FlightTimeInterceptor.execute(sync, new FlightTimeInterceptor.Callable<Integer>()
                {
                    @Override
                    public Integer call() throws RuntimeException
                    {
                        return pullRestpoint.getCount(commentsUrl);
                    }
                });
            }
        }
        RepositoryPullRequestMapping local = dao.findRequestByRemoteId(repo, remoteId);

        // don't have this pull request, let's save it
        if (local == null)
        {
            local = dao.savePullRequest(repo, toDaoModelPullRequest(remote, repo, commentCount));
        }

        // maybe update
        if (remote != null && hasChanged(local, remote, commentCount))
        {
            String sourceBranch = checkNotNull(getBranchName(remote.getSource(), local.getSourceBranch()), "Source branch");
            String dstBranch = checkNotNull(getBranchName(remote.getDestination(), local.getDestinationBranch()), "Destination branch");

            local = dao.updatePullRequestInfo(local.getID(), remote.getTitle(),
                    sourceBranch, dstBranch,
                    resolveBitbucketStatus(remote.getState()),
                    remote.getUpdatedOn(), getRepositoryFullName(remote.getSource().getRepository()), commentCount
            );
        }

        if (participantIndex != null)
        {
            pullRequestService.updatePullRequestParticipants(local.getID(), repo.getId(), participantIndex);
        }

        return local;
    }

    private String checkNotNull(String branch, String object)
    {
        if (branch == null)
        {
            throw new IllegalStateException(object + " must not be null");
        }

        return branch;
    }

    private String getBranchName(BitbucketPullRequestHead ref, String oldBranchName)
    {
        if (ref == null || ref.getBranch() == null || ref.getBranch().getName() == null)
        {
            return oldBranchName;
        }

        return ref.getBranch().getName();
    }

    private String getRepositoryFullName(BitbucketPullRequestRepository repository)
    {
        // in case that fork has been deleted, the source repository is null
        if (repository != null)
        {
            return repository.getFullName();
        }

        return null;
    }

    private RepositoryPullRequestMapping.Status resolveBitbucketStatus(String string)
    {
        for (RepositoryPullRequestMapping.Status status : RepositoryPullRequestMapping.Status.values())
        {
            if (status.name().equalsIgnoreCase(string))
            {
                return status;
            }
        }
        return RepositoryPullRequestMapping.Status.OPEN;
    }

    private Map<String, Participant> loadPulRequestParticipants(final PullRequestRemoteRestpoint pullRestpoint, final BitbucketPullRequest remote)
    {
        List<BitbucketPullRequestParticipant> participants = remote.getParticipants();

        Map<String, Participant> participantsIndex = new LinkedHashMap<String, Participant>();

        if (participants != null)
        {
            for (BitbucketPullRequestParticipant bitbucketParticipant : participants)
            {
                Participant participant = new Participant(bitbucketParticipant.getUser().getUsername(), bitbucketParticipant.isApproved(), bitbucketParticipant.getRole());
                participantsIndex.put(bitbucketParticipant.getUser().getUsername(), participant);
            }
        } else
        {
            //FIXME we fallback to reviewers and approvals links if participants are not present, remove it after participants are at production
            BitbucketLink reviewersLink = remote.getLinks().getReviewers();
            BitbucketLink approvalsLink = remote.getLinks().getApprovals();

            if (reviewersLink != null)
            {
                Iterable<BitbucketPullRequestReviewer> bitbucketReviewers = pullRestpoint.getPullRequestReviewers(reviewersLink.getHref());
                for (BitbucketPullRequestReviewer bitbucketReviewer : bitbucketReviewers)
                {
                    Participant participant = new Participant(bitbucketReviewer.getUser().getUsername(), false, REVIEWER_ROLE);
                    participantsIndex.put(bitbucketReviewer.getUser().getUsername(), participant);
                }
            }

            if (approvalsLink != null)
            {
                Iterable<BitbucketPullRequestApprovalActivity> bitbucketApprovals = pullRestpoint.getPullRequestApprovals(approvalsLink.getHref());
                for (BitbucketPullRequestApprovalActivity bitbucketApproval : bitbucketApprovals)
                {
                    Participant participant = participantsIndex.get(bitbucketApproval.getUser().getUsername());
                    if (participant == null)
                    {
                        participantsIndex.put(bitbucketApproval.getUser().getUsername(), new Participant(bitbucketApproval.getUser().getUsername(), true, REVIEWER_ROLE));
                    } else
                    {
                        participant.setApproved(true);
                    }
                }
            }
        }

        return participantsIndex;
    }

    private boolean hasChanged(RepositoryPullRequestMapping local, BitbucketPullRequest remote, int commentCount)
    {
        // the pull request has changed if it was updated or it is the same but comments changed
        return remote.getUpdatedOn().after(local.getUpdatedOn())
                || local.getCommentCount() != commentCount;
    }

    private void loadPullRequestCommits(final Repository repo, final PullRequestRemoteRestpoint pullRestpoint,
            final int localPullRequestId, final BitbucketPullRequestUpdateActivity activity, final RepositoryPullRequestMapping savedPullRequest, final BitbucketPullRequest remotePullRequest)
    {
        if (activity.getSource() != null && activity.getSource().getRepository() != null)
        {
            final Progress sync = repo.getSync();

            FlightTimeInterceptor.execute(sync, new FlightTimeInterceptor.Callable<Void>()
            {
                @Override
                public Void call()
                {
                    try
                    {
                        Iterable<BitbucketPullRequestCommit> commitsIterator = getCommits(repo, remotePullRequest, pullRestpoint);

                        for (BitbucketPullRequestCommit commit : commitsIterator)
                        {
                            RepositoryCommitMapping localCommit = dao.getCommitByNode(repo, localPullRequestId, commit.getHash());
                            if (localCommit == null)
                            {
                                localCommit = saveCommit(repo, commit, null, localPullRequestId);
                                linkCommit(repo, localCommit, savedPullRequest);
                            } else {
                                break;
                            }
                        }
                    } catch(BitbucketRequestException.NotFound_404 e)
                    {
                        LOGGER.info("There are no commits for pull request", e);
                    }

                    return null;
                }
            });
        } else
        {
            LOGGER.debug("The source repository is not available for pull request [{}]. Skipping loading commits.", remotePullRequest.getId());
        }
    }

    private Iterable<BitbucketPullRequestCommit> getCommits(Repository repo, BitbucketPullRequest remotePullRequest, PullRequestRemoteRestpoint pullRestpoint)
    {
        Iterable<BitbucketPullRequestCommit> commitsIterator;
        BitbucketLink commitsLink = remotePullRequest.getLinks().getCommits();
        if (commitsLink != null && !StringUtils.isBlank(commitsLink.getHref()))
        {
            commitsIterator = pullRestpoint.getPullRequestCommits(commitsLink.getHref());
        } else
        {
            // if there is no commits link, fall back to use generated commits url
            commitsIterator = pullRestpoint.getPullRequestCommits(repo.getOrgName(), repo.getSlug(), remotePullRequest.getId() + "");
        }

        return commitsIterator;
    }

    private void linkCommit(Repository domainRepository, RepositoryCommitMapping commitMapping,
            RepositoryPullRequestMapping request)
    {
        dao.linkCommit(domainRepository, request, commitMapping);
    }


    private RepositoryCommitMapping saveCommit(Repository domainRepository, BitbucketPullRequestCommit commit, String nextNode, int localPullRequestId)
    {
        if (commit != null)
        {
            return dao.saveCommit(domainRepository, toDaoModelCommit(commit));
        }
        return null;
    }

    private Map<String, Object> toDaoModelCommit(BitbucketPullRequestCommit commit)
    {
        HashMap<String, Object> ret = new HashMap<String, Object>();
        if (commit.getAuthor().getUser() != null)
        {
            ret.put(RepositoryCommitMapping.AUTHOR, commit.getAuthor().getUser().getUsername());
        } else
        {
            ret.put(RepositoryCommitMapping.AUTHOR, commit.getAuthor().getRaw().replaceAll("<[^>]*>", "").trim());
        }
        ret.put(RepositoryCommitMapping.RAW_AUTHOR, commit.getAuthor().getRaw());
        ret.put(RepositoryCommitMapping.MESSAGE, commit.getMessage());
        ret.put(RepositoryCommitMapping.NODE, commit.getHash());
        ret.put(RepositoryCommitMapping.DATE, commit.getDate());

        return ret;
    }


    private Map<String, Object> toDaoModelPullRequest(BitbucketPullRequest request, Repository repository, int commentCount)
    {
        String sourceBranch = checkNotNull(getBranchName(request.getSource(), null), "Source branch");
        String dstBranch = checkNotNull(getBranchName(request.getDestination(), null), "Destination branch");

        HashMap<String, Object> ret = new HashMap<String, Object>();
        ret.put(RepositoryPullRequestMapping.REMOTE_ID, request.getId());
        ret.put(RepositoryPullRequestMapping.NAME, ActiveObjectsUtils.stripToLimit(request.getTitle(), 255));
        ret.put(RepositoryPullRequestMapping.URL, request.getLinks().getHtml().getHref());
        ret.put(RepositoryPullRequestMapping.TO_REPO_ID, repository.getId());

        String author = null;
        if (request.getAuthor() != null)
        {
            author = request.getAuthor().getUsername();
        }

        ret.put(RepositoryPullRequestMapping.AUTHOR, author);
        ret.put(RepositoryPullRequestMapping.CREATED_ON, request.getCreatedOn());
        ret.put(RepositoryPullRequestMapping.UPDATED_ON, request.getUpdatedOn());
        ret.put(RepositoryPullRequestMapping.DESTINATION_BRANCH, dstBranch);
        ret.put(RepositoryPullRequestMapping.SOURCE_BRANCH, sourceBranch);
        ret.put(RepositoryPullRequestMapping.SOURCE_REPO, getRepositoryFullName(request.getSource().getRepository()));
        ret.put(RepositoryPullRequestMapping.LAST_STATUS, resolveBitbucketStatus(request.getState()).name());
        ret.put(RepositoryPullRequestMapping.COMMENT_COUNT, commentCount);

        return ret;
    }

    private boolean isUpdateActivity(BitbucketPullRequestBaseActivity activity)
    {
        return activity instanceof BitbucketPullRequestUpdateActivity
                && RepositoryPullRequestMapping.Status.OPEN.name().equalsIgnoreCase(((BitbucketPullRequestUpdateActivity) activity).getState());
    }

    @Override
    public String getQueue()
    {
        return ID;
    }

    @Override
    public MessageAddress<BitbucketSynchronizeActivityMessage> getAddress()
    {
        return messagingService.get(BitbucketSynchronizeActivityMessage.class, KEY);
    }

    @Override
    public int getParallelThreads()
    {
        return MessageConsumer.THREADS_PER_CONSUMER;
    }
}
