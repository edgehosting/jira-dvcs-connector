package com.atlassian.jira.plugins.dvcs.spi.gitlab;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabCommit;
import org.gitlab.api.models.GitlabCommitDiff;
import org.gitlab.api.models.GitlabProject;
import org.gitlab.api.models.GitlabProjectHook;
import org.gitlab.api.models.GitlabUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Branch;
import com.atlassian.jira.plugins.dvcs.model.BranchHead;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileAction;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetail;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetailsEnvelope;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.BranchService;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.gitlab.message.SynchronizeChangesetMessage;
import com.atlassian.jira.plugins.dvcs.sync.GitlabSynchronizeChangesetMessageConsumer;
import com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class GitlabCommunicator implements DvcsCommunicator {
    public static final String GITLAB = "gitlab";
    
    private static final Logger LOG = LoggerFactory.getLogger(GitlabCommunicator.class);
	    
    @Resource
    private MessagingService messagingService;
    
    @Resource
    private BranchService branchService;

	@Override
	public void ensureHookPresent(Repository repository, String postCommitUrl) {
		LOG.info("ensureHookPresent: repo={}, postCommitUrl={}", repository, postCommitUrl);
		
		GitlabAPI gitlabAPI = GitlabAPI.connect(repository.getOrgHostUrl(), repository.getCredential().getAccessToken());
		
		try {
			GitlabProject project = gitlabAPI.getProject(getGitlabProjectId(repository));
			
			List<GitlabProjectHook> projectHooks = gitlabAPI.getProjectHooks(project);
			for (GitlabProjectHook hook : projectHooks) {
				if (hook.getUrl().equals(postCommitUrl)) {
					LOG.info("Hook is already present");
					return;
				}
			}

			gitlabAPI.addProjectHook(project, postCommitUrl);
		} catch (NumberFormatException e) {
			LOG.error("Problem with ensureHookPresent", e);
		} catch (IOException e) {
			LOG.error("Problem with ensureHookPresent", e);
		}
	}

	@Override
	public AccountInfo getAccountInfo(String hostUrl, String accountName) {
		LOG.info("getAccountInfo: url={}, name={}", hostUrl, accountName);
		
		// TODO Really verify that user exists
		return new AccountInfo(GITLAB);
	}

	@Override
	public String getBranchUrl(Repository repository, Branch branch) {	
		LOG.info("getBranchUrl: repo={}, branch={}", repository, branch);
		
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Branch> getBranches(Repository repository) {
		LOG.info("getBranches: repo={}", repository);
		
		List<Branch> branches = new ArrayList<Branch>();
		
		GitlabAPI gitlabAPI = GitlabAPI.connect(repository.getOrgHostUrl(), repository.getCredential().getAccessToken());

		try {
			GitlabProject project = gitlabAPI.getProject(getGitlabProjectId(repository));
			List<GitlabBranch> glBranches = gitlabAPI.getBranches(project);
			
			for (GitlabBranch glBranch : glBranches) {
				List<BranchHead> branchHeads = new ArrayList<BranchHead>();
				BranchHead branchTip = new BranchHead(glBranch.getName(), glBranch.getCommit().getId());
                branchHeads.add(branchTip);
				
                Branch branch = new Branch(glBranch.getName());
				branch.setHeads(branchHeads);
				branch.setRepositoryId(repository.getId());
                
				branches.add(branch);
			}
		} catch (NumberFormatException e) {
			LOG.error("Problem while getBranches", e);
		} catch (IOException e) {
			LOG.error("Problem while getBranches", e);
		}
		
		return branches;
	}

	@Override
	public Changeset getChangeset(Repository repository, String node) {
		LOG.info("getChangeset: repo={}, node={}", repository, node);
		
		GitlabAPI gitlabAPI = GitlabAPI.connect(repository.getOrgHostUrl(), repository.getCredential().getAccessToken());
		
		try {
			GitlabCommit commit = gitlabAPI.getCommit(getGitlabProjectId(repository), node);
			
			LOG.info("commit title={} description={}", commit.getTitle(), commit.getDescription());
			
			Changeset changeset = new Changeset(repository.getId(), commit.getId(), commit.getTitle(), commit.getCreatedAt());
			
			changeset.setAuthor(commit.getAuthorName());
			changeset.setAuthorEmail(commit.getAuthorEmail());
			changeset.setParents(commit.getParentIds());
		
			List<ChangesetFile> files = new ArrayList<ChangesetFile>();
			List<ChangesetFileDetail> fileDetails = new ArrayList<ChangesetFileDetail>();
			
			List<GitlabCommitDiff> commitDiffs = gitlabAPI.getCommitDiffs(getGitlabProjectId(repository), commit.getId());
			for (GitlabCommitDiff commitDiff : commitDiffs) {
				if (commitDiff.getNewFile()) {
					files.add(new ChangesetFile(ChangesetFileAction.ADDED, commitDiff.getNewPath()));
					fileDetails.add(new ChangesetFileDetail(ChangesetFileAction.ADDED, commitDiff.getNewPath(), 1, 1));
				} else if (commitDiff.getDeletedFile()) {
					files.add(new ChangesetFile(ChangesetFileAction.REMOVED, commitDiff.getOldPath()));
					fileDetails.add(new ChangesetFileDetail(ChangesetFileAction.REMOVED, commitDiff.getOldPath(), 1, 1));
				} else if (commitDiff.getRenamedFile()) {
					files.add(new ChangesetFile(ChangesetFileAction.REMOVED, commitDiff.getOldPath()));					
					fileDetails.add(new ChangesetFileDetail(ChangesetFileAction.REMOVED, commitDiff.getOldPath(), 1, 1));

					files.add(new ChangesetFile(ChangesetFileAction.ADDED, commitDiff.getNewPath()));
					fileDetails.add(new ChangesetFileDetail(ChangesetFileAction.ADDED, commitDiff.getNewPath(), 1, 1));
				} else {
					int additions = StringUtils.countMatches(commitDiff.getDiff(), "\n+") - StringUtils.countMatches(commitDiff.getDiff(), "\n+++");
					int deletions = StringUtils.countMatches(commitDiff.getDiff(), "\n-") - StringUtils.countMatches(commitDiff.getDiff(), "\n---");
					
					files.add(new ChangesetFile(ChangesetFileAction.MODIFIED, commitDiff.getNewPath()));
					fileDetails.add(new ChangesetFileDetail(ChangesetFileAction.MODIFIED, commitDiff.getNewPath(), additions, deletions));
				}
			}
			
			changeset.setFiles(files);
			changeset.setFileDetails(fileDetails);
			changeset.setAllFileCount(files.size());

			return changeset;
			
		} catch (IOException e) {
			LOG.error("Problem while getChangeset", e);
		}
		
		return null;
	}

	private String getGitlabProjectId(Repository repository) {
		return repository.getOrgName() + "/" + repository.getSlug();
	}

	@Override
	public String getCommitUrl(Repository repository, Changeset changeset) {
		LOG.info("getCommitUrl: repo={}, changeset={}", repository, changeset);
		
		return repository.getRepositoryUrl() + "/commit/" + changeset.getNode();
	}

	@Override
	public String getCreatePullRequestUrl(Repository repository, String sourceSlug, final String sourceBranch, String destinationSlug, final String destinationBranch, String eventSource) {
		LOG.info("getCreatePullRequestUrl: repo={}, sourceSlug={}, sourceBranch={}, destinationSlug={}, destinationBranch={}, eventSource={}", new Object[] { repository, sourceSlug, sourceBranch, destinationSlug, destinationBranch, eventSource});
		
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDvcsType() {
		return GITLAB;
	}

	@Override
	public String getFileCommitUrl(Repository repository, Changeset changeset, String file, int index) {
		LOG.info("getFileCommitUrl: repo={}, changeset={}, file={}, index={}", new Object[] { repository, changeset, file, index });
		
		return repository.getRepositoryUrl() + "/commit/" + changeset.getNode() + "#diff-" + index;
	}

	@Override
	public ChangesetFileDetailsEnvelope getFileDetails(Repository repository, Changeset changeset) {
		LOG.info("getFileDetails: repo={}, changeset={}", repository, changeset);
		
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Group> getGroupsForOrganization(Organization organization) {
		LOG.info("getGroupsForOrganization: organization={}");
		
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Repository> getRepositories(Organization organization, List<Repository> storedRepositories) {
		LOG.info("getRepositories: organization={}, storedRepositories={}", organization, storedRepositories);
		
        ImmutableMap<String, Repository> storedReposMap = Maps.uniqueIndex(storedRepositories, new Function<Repository, String>()
        {
            @Override
            public String apply(Repository r)
            {
                return r.getSlug();
            }
        });

		
		GitlabAPI gitlabAPI = GitlabAPI.connect(organization.getHostUrl(), organization.getCredential().getAccessToken());
		try {
			List<GitlabProject> projects = gitlabAPI.getProjects();
			
			List<Repository> repos = new ArrayList<Repository>();
			
			for (GitlabProject gitlabProject : projects) {
				if (gitlabProject.getNamespace().getPath().equals(organization.getName())) {
					if (storedReposMap.containsKey(gitlabProject.getName())) {
						// Repo was already stored
						repos.add(storedReposMap.get(gitlabProject.getName()));
					} else {
						// Repo is new
						Repository repo = new Repository();
						repo.setSlug(gitlabProject.getName());
						repo.setName(gitlabProject.getName());
						
						repos.add(repo);
					}
				}
			}
			
			return repos;
		} catch (IOException e) {
			LOG.error("Communication with GitLab failed", e);
		}
		
		return null;
	}

	@Override
	public DvcsUser getTokenOwner(Organization organization) {
		LOG.info("getTokenOwner: organization={}", organization);
		
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DvcsUser getUser(Repository repository, String author) {
		LOG.info("getUser: repo={}, author={}", repository, author);
		
		GitlabAPI gitlabAPI = GitlabAPI.connect(repository.getOrgHostUrl(), repository.getCredential().getAccessToken());

		try {
			List<GitlabUser> users = gitlabAPI.getUsers();
			for (GitlabUser user : users) {
				if (author.equals(user.getName())) {
					String userUrl = repository.getOrgHostUrl() + "/u/" + user.getUsername();
					
					DvcsUser dvcsUser = new DvcsUser(user.getUsername(), user.getName(), user.getName(), user.getAvatarUrl(), userUrl);
					return dvcsUser;
				}
			}
			
		} catch (IOException e) {
			LOG.error("Problem while getUser", e);
		}
		
		return null;
	}

	@Override
	public boolean isSyncDisabled(Repository repo, EnumSet<SynchronizationFlag> flags) {
		LOG.info("isSyncDisabled: repo={}, flags={}", repo, flags);

		// Never disable sync automatically
		
		return false;
	}

	@Override
	public void linkRepository(Repository repository, Set<String> withProjectkeys) {
		LOG.info("linkRepository: repo={}, withProjectKeys={}", repository, withProjectkeys);
		// Do nothing
	}

	@Override
	public void removePostcommitHook(Repository repository, String postCommitUrl) {
		LOG.info("removePostcommitHook: repo={}, postCommitUrl={}", repository, postCommitUrl);
		
		GitlabAPI gitlabAPI = GitlabAPI.connect(repository.getOrgHostUrl(), repository.getCredential().getAccessToken());
		
		try {
			GitlabProject project = gitlabAPI.getProject(getGitlabProjectId(repository));
			
			List<GitlabProjectHook> projectHooks = gitlabAPI.getProjectHooks(project);
			for (GitlabProjectHook hook : projectHooks) {
				if (hook.getUrl().equals(postCommitUrl)) {
					gitlabAPI.deleteProjectHook(project, hook.getId());
					return;
				}
			}
		} catch (NumberFormatException e) {
			LOG.error("Problem with ensureHookPresent", e);
		} catch (IOException e) {
			LOG.error("Problem with ensureHookPresent", e);
		}
	}

	@Override
	public void startSynchronisation(Repository repo, EnumSet<SynchronizationFlag> flags, int auditId) {
		LOG.info("startSynchronisation: repo={}, flags={}, auditId={}", new Object[] { repo, flags, auditId });
		
        final boolean softSync = flags.contains(SynchronizationFlag.SOFT_SYNC);
        final boolean webHookSync = flags.contains(SynchronizationFlag.WEBHOOK_SYNC);
        final boolean changestesSync = flags.contains(SynchronizationFlag.SYNC_CHANGESETS);
        final boolean pullRequestSync = flags.contains(SynchronizationFlag.SYNC_PULL_REQUESTS);

        String[] synchronizationTags = new String[] { messagingService.getTagForSynchronization(repo), messagingService.getTagForAuditSynchronization(auditId) };
        if (changestesSync || softSync)
        {
            Date synchronizationStartedAt = new Date();
            List<Branch> branches = getBranches(repo);
            for (Branch branch : branches)
            {
                for (BranchHead branchHead : branch.getHeads())
                {
                    SynchronizeChangesetMessage message = new SynchronizeChangesetMessage(repo, //
                            branch.getName(), branchHead.getHead(), //
                            synchronizationStartedAt, //
                            null, softSync, auditId, webHookSync);
                    MessageAddress<SynchronizeChangesetMessage> key = messagingService.get( //
                            SynchronizeChangesetMessage.class, //
                            GitlabSynchronizeChangesetMessageConsumer.ADDRESS //
                    );
                    messagingService.publish(key, message, softSync ? MessagingService.SOFTSYNC_PRIORITY : MessagingService.DEFAULT_PRIORITY, messagingService.getTagForSynchronization(repo), messagingService.getTagForAuditSynchronization(auditId));
                }
            }
            List<BranchHead> oldBranchHeads = branchService.getListOfBranchHeads(repo);
            branchService.updateBranchHeads(repo, branches, oldBranchHeads);
            branchService.updateBranches(repo, branches);
        }
        if (pullRequestSync)
        {
        	LOG.info("Pull request sync not implemented yet");
        	/*
            GitlabPullRequestPageMessage message = new GitlabPullRequestPageMessage(null, auditId, softSync, repo, PagedRequest.PAGE_FIRST, PULLREQUEST_PAGE_SIZE, null, webHookSync);
            MessageAddress<GitlabPullRequestPageMessage> key = messagingService.get(
                    GitlabPullRequestPageMessage.class,
                    GitlabPullRequestPageMessageConsumer.ADDRESS
            );
            messagingService.publish(key, message, messagingService.getTagForSynchronization(repo), messagingService.getTagForAuditSynchronization(auditId));
            */
        }		
	}

	@Override
	public boolean supportsInvitation(Organization organization) {
		LOG.info("supportsInvitation: organization={}", organization);
		
		// No invitation support
		return false;
	}

	@Override
	public void inviteUser(Organization organization, Collection<String> groupSlugs, String userEmail) {
		LOG.info("inviteUser: organization={}, groupSlugs={}, userEmail={}", new Object[] { organization, groupSlugs, userEmail });
		
		// No invitation support
	}

}
