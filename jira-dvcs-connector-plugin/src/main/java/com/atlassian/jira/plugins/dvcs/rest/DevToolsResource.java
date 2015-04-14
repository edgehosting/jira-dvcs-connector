package com.atlassian.jira.plugins.dvcs.rest;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Participant;
import com.atlassian.jira.plugins.dvcs.model.PullRequest;
import com.atlassian.jira.plugins.dvcs.model.PullRequestRef;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.model.dev.RestChangeset;
import com.atlassian.jira.plugins.dvcs.model.dev.RestChangesetRepository;
import com.atlassian.jira.plugins.dvcs.model.dev.RestDevResponse;
import com.atlassian.jira.plugins.dvcs.model.dev.RestParticipant;
import com.atlassian.jira.plugins.dvcs.model.dev.RestPrCommit;
import com.atlassian.jira.plugins.dvcs.model.dev.RestPrRepository;
import com.atlassian.jira.plugins.dvcs.model.dev.RestPullRequest;
import com.atlassian.jira.plugins.dvcs.model.dev.RestRef;
import com.atlassian.jira.plugins.dvcs.model.dev.RestRepository;
import com.atlassian.jira.plugins.dvcs.model.dev.RestUser;
import com.atlassian.jira.plugins.dvcs.rest.security.AuthorizationException;
import com.atlassian.jira.plugins.dvcs.service.BranchService;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.PullRequestService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.webwork.IssueAndProjectKeyManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.software.api.permissions.SoftwareProjectPermissions;
import com.atlassian.plugins.rest.common.Status;
import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * The DevTools Resource.
 */
@Path("/jira-dev")
public class DevToolsResource
{
    /** The log. */
    private final Logger log = LoggerFactory.getLogger(DevToolsResource.class);

    /** The repository service. */
    private final RepositoryService repositoryService;

    private final ChangesetService changesetService;

    private final PullRequestService pullRequestService;

    private final BranchService branchService;

    private final IssueAndProjectKeyManager issueAndProjectKeyManager;

    /**
     * The Constructor.
     *
     * @param repositoryService
     * @param changesetService
     * @param pullRequestService
     * @param branchService
     * @param issueAndProjectKeyManager
     */
    public DevToolsResource(RepositoryService repositoryService, ChangesetService changesetService,
            PullRequestService pullRequestService, BranchService branchService, IssueAndProjectKeyManager issueAndProjectKeyManager)
    {
        this.repositoryService = repositoryService;
        this.changesetService = changesetService;
        this.pullRequestService = pullRequestService;
        this.branchService = branchService;
        this.issueAndProjectKeyManager = issueAndProjectKeyManager;
    }

    @GET
    @Path("/detail")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getCommits(@QueryParam("issue") String issueKey)
    {
        return new RestTransformer<RestChangesetRepository>()
        {
            private ListMultimap<Integer, Changeset> changesetTorepositoryMapping;

            @Override
            protected Set<Integer> getRepositories(final Set<String> issueKeys)
            {
                List<Changeset> changesets = changesetService.getByIssueKey(issueKeys, true);

                changesetTorepositoryMapping = ArrayListMultimap.create();

                // group changesets by repository
                for (Changeset changeset : changesets)
                {
                    for (int repositoryId : changeset.getRepositoryIds())
                    {
                        changesetTorepositoryMapping.put(repositoryId, changeset);
                    }
                }

                return changesetTorepositoryMapping.keySet();
            }

            @Override
            protected RestChangesetRepository createRepository()
            {
                return new RestChangesetRepository();
            }

            @Override
            protected void setData(final RestChangesetRepository restRepository, final Repository repository)
            {
                restRepository.setCommits(createCommits(repository, changesetTorepositoryMapping.get(repository.getId())));
            }

        }.getResponse(issueKey);
    }

    private List<RestChangeset> createCommits(Repository repository, List<Changeset> changesets)
    {
        List<RestChangeset> restChangesets = new ArrayList<RestChangeset>();
        for (Changeset changeset : changesets)
        {
            DvcsUser user = repositoryService.getUser(repository, changeset.getAuthor(), changeset.getRawAuthor());
            RestChangeset restChangeset = new RestChangeset();
            restChangeset.setAuthor(new RestUser(user.getUsername(), user.getFullName(), changeset.getAuthorEmail(), user.getAvatar()));
            restChangeset.setAuthorTimestamp(changeset.getDate().getTime());
            restChangeset.setDisplayId(changeset.getNode().substring(0, 7));
            restChangeset.setId(changeset.getRawNode());
            restChangeset.setMessage(changeset.getMessage());
            restChangeset.setFileCount(changeset.getAllFileCount());
            restChangeset.setUrl(changesetService.getCommitUrl(repository, changeset));

            if (changeset.getParents() == null)
            {
                // no parents are set, it means that the length of the parent json is too long, so it was large merge (e.g Octopus merge)
                restChangeset.setMerge(true);
            } else
            {
                restChangeset.setMerge(changeset.getParents().size() > 1);
            }

            restChangesets.add(restChangeset);
        }

        return restChangesets;
    }

    @GET
    @Path("/pr-detail")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getPullRequests(@QueryParam("issue") String issueKey)
    {
        return new RestTransformer<RestPrRepository>()
        {
            private ListMultimap<Integer, PullRequest> prTorepositoryMapping;

            @Override
            protected Set<Integer> getRepositories(final Set<String> issueKeys)
            {
                List<PullRequest> pullRequests = pullRequestService.getByIssueKeys(issueKeys, true);

                prTorepositoryMapping = Multimaps.index(pullRequests, new Function<PullRequest, Integer>()
                {
                    @Override
                    public Integer apply(@Nullable final PullRequest input)
                    {
                        return input.getRepositoryId();
                    }
                });

                return prTorepositoryMapping.keySet();
            }

            @Override
            protected RestPrRepository createRepository()
            {
                return new RestPrRepository();
            }

            @Override
            protected void setData(final RestPrRepository restRepository, final Repository repository)
            {
                restRepository.setPullRequests(createPullRequests(repository, prTorepositoryMapping.get(repository.getId())));
            }

        }.getResponse(issueKey);
    }

    private List<RestPullRequest> createPullRequests(final Repository repository, final List<PullRequest> pullRequests)
    {
        List<RestPullRequest> restPullRequests = new ArrayList<RestPullRequest>();
        for (PullRequest pullRequest : pullRequests)
        {
            DvcsUser user = repositoryService.getUser(repository, pullRequest.getAuthor(), pullRequest.getAuthor());
            RestPullRequest restPullRequest = new RestPullRequest();
            restPullRequest.setAuthor(new RestUser(user.getUsername(), user.getFullName(), null, user.getAvatar()));
            restPullRequest.setCreatedOn(pullRequest.getCreatedOn().getTime());
            restPullRequest.setTitle(pullRequest.getName());
            restPullRequest.setId(pullRequest.getRemoteId());
            restPullRequest.setUrl(pullRequest.getUrl());
            restPullRequest.setUpdatedOn(pullRequest.getUpdatedOn().getTime());
            restPullRequest.setStatus(pullRequest.getStatus().name());
            restPullRequest.setSource(createRef(pullRequest.getSource()));
            restPullRequest.setDestination(createRef(pullRequest.getDestination()));
            restPullRequests.add(restPullRequest);
            restPullRequest.setParticipants(createParticipants(repository, pullRequest.getParticipants()));
            restPullRequest.setCommentCount(pullRequest.getCommentCount());
            restPullRequest.setCommits(createPrCommits(pullRequest.getCommits()));
        }

        return restPullRequests;
    }

    private List<RestParticipant> createParticipants(final Repository repository, final List<Participant> participants)
    {
        List<RestParticipant> restParticipants = new ArrayList<RestParticipant>();

        for (Participant participant : participants)
        {
            DvcsUser user = repositoryService.getUser(repository, participant.getUsername(), participant.getUsername());
            RestParticipant restParticipant = new RestParticipant(new RestUser(user.getUsername(), user.getFullName(), null, user.getAvatar()), participant.isApproved(), participant.getRole());
            restParticipants.add(restParticipant);
        }
        return restParticipants;
    }

    private RestRef createRef(PullRequestRef ref)
    {
        if (ref == null)
        {
            return null;
        }
        RestRef restRef = new RestRef();
        restRef.setBranch(ref.getBranch());
        restRef.setRepository(ref.getRepository());
        restRef.setUrl(ref.getRepositoryUrl());

        return restRef;
    }

    private List<RestPrCommit> createPrCommits(final List<Changeset> prCommits)
    {
        if (prCommits == null)
        {
            return null;
        }

        List<RestPrCommit> restPrCommits = new ArrayList<RestPrCommit>();

        for (Changeset prCommit : prCommits)
        {
            RestPrCommit restPrCommit = new RestPrCommit();
            restPrCommit.setRawAuthor(prCommit.getRawAuthor());
            restPrCommit.setAuthor(prCommit.getAuthor());
            restPrCommit.setDate(prCommit.getDate());
            restPrCommit.setMessage(prCommit.getMessage());
            restPrCommit.setNode(prCommit.getNode());

            restPrCommits.add(restPrCommit);
        }

        return restPrCommits;
    }

    private abstract class RestTransformer<T extends RestRepository>
    {
        public Response getResponse(String issueKey)
        {
            Issue issue = issueAndProjectKeyManager.getIssue(issueKey);
            if (issue == null)
            {
                return Status.notFound().message("Issue not found").response();
            }

            if (!issueAndProjectKeyManager.hasIssuePermission(ProjectPermissions.BROWSE_PROJECTS, issue))
            {
                throw new AuthorizationException();
            }

            Project project = issue.getProjectObject();

            if (project == null)
            {
                return Status.notFound().message("Project was not found").response();
            }

            if (!issueAndProjectKeyManager.hasProjectPermission(SoftwareProjectPermissions.VIEW_DEV_TOOLS, project))
            {
                throw new AuthorizationException();
            }

            Set<String> issueKeys = issueAndProjectKeyManager.getAllIssueKeys(issue);
            Set<Integer> repositoryIds = getRepositories(issueKeys);
            Map<Integer, Repository> repositories = new HashMap<Integer, Repository>();

            List<T> restRepositories = new ArrayList<T>();
            for (int repositoryId : repositoryIds)
            {
                Repository repository = repositories.get(repositoryId);

                if (repository == null)
                {
                    repository = repositoryService.get(repositoryId);
                    repositories.put(repositoryId, repository);
                }

                T restRepository = createRepository();
                restRepository.setName(repository.getName());
                restRepository.setSlug(repository.getSlug());
                restRepository.setUrl(repository.getRepositoryUrl());
                restRepository.setAvatar(repository.getLogo());
                setData(restRepository, repository);
                restRepository.setFork(repository.isFork());
                if (repository.isFork() && repository.getForkOf() != null)
                {
                    RestRepository forkOfRepository = new RestChangesetRepository();
                    forkOfRepository.setName(repository.getForkOf().getName());
                    forkOfRepository.setSlug(repository.getForkOf().getSlug());
                    forkOfRepository.setUrl(repository.getForkOf().getRepositoryUrl());
                    restRepository.setForkOf(forkOfRepository);
                }

                restRepositories.add(restRepository);
            }

            RestDevResponse result = new RestDevResponse();
            result.setRepositories(restRepositories);
            return Response.ok(result).build();
        }

        protected abstract Set<Integer> getRepositories(final Set<String> issueKeys);
        protected abstract T createRepository();
        protected abstract void setData(T restRepository, Repository repository);

    }

    @GET
    @Path("/branch")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getBranches(@QueryParam("issue") String issueKey)
    {
        Issue issue = issueAndProjectKeyManager.getIssue(issueKey);
        if (issue == null)
        {
            return Status.notFound().message("Issue not found").response();
        }

        if (!issueAndProjectKeyManager.hasIssuePermission(ProjectPermissions.BROWSE_PROJECTS, issue))
        {
            throw new AuthorizationException();
        }

        Project project = issue.getProjectObject();

        if (project == null)
        {
            return Status.notFound().message("Project was not found").response();
        }

        if (!issueAndProjectKeyManager.hasProjectPermission(SoftwareProjectPermissions.VIEW_DEV_TOOLS, project))
        {
            throw new AuthorizationException();
        }

        Set<String> issueKeys = issueAndProjectKeyManager.getAllIssueKeys(issue);

        Map<String,Object> result = new HashMap<String, Object>();
        result.put("branches", branchService.getByIssueKey(issueKeys));
        return Response.ok(result).build();
    }
}
