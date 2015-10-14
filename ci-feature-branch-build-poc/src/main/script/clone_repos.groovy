import groovy.transform.Field
import org.kohsuke.github.GitHub
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// TODO: get the list of KIE repos from the repository-list.txt ?
@Field
def static final List<String> ALL_REPOS = [
        "uberfire",
        "uberfire-extensions",
        "dashbuilder",
        "droolsjbpm-build-bootstrap",
        "droolsjbpm-knowledge",
        "drools",
        "optaplanner",
        "jbpm",
        "droolsjbpm-integration",
        "droolsjbpm-tools",
        "kie-uberfire-extensions",
        "guvnor",
        "kie-wb-common",
        "jbpm-form-modeler",
        "drools-wb",
        "jbpm-designer",
        "jbpm-console-ng",
        "dashboard-builder",
        "optaplanner-wb",
        "jbpm-dashboard",
        "optaplanner-wb",
        "kie-docs",
        "kie-wb-distributions",
        "droolsjbpm-build-distribution",
        "kie-eap-modules"
]

@Field
static final Logger logger = LoggerFactory.getLogger(getClass())

@Field
static final String ACCESS_TOKEN = System.getProperty("ghAccessToken")

@Field
static final int PR_ID = System.getProperty("ghprbPullId").toInteger()

@Field
static final String SOURCE_BRANCH = System.getProperty("ghprbSourceBranch")

@Field
static final String TARGET_BRANCH = System.getProperty("ghprbTargetBranch")

@Field
static final String PR_LINK = System.getProperty("ghprbPullLink")

@Field
static final File BASEDIR = new File(System.getProperty("basedir"))

@Field
/** Full repo name -- always needs to include the owner prefix. e.g. not just "uberfire" but "uberfire/uberfire" */
static final String FULL_REPO_NAME = System.getProperty("ghRepoName")

@Field
GitHub github = GitHub.connectUsingOAuth(ACCESS_TOKEN)

logArgs()

def repo = github.getRepository(FULL_REPO_NAME)
def pr = repo.getPullRequest(PR_ID)
/** Fork name is the repository where the PR branch resides. It may differ from username of the PR submitter */
def forkName = pr.getHead().getRepository().getOwner().getLogin()
def repoName = repo.getName()
def upstreamRepos = getUpstreamRepos(repoName)

println("Upstream repositories: $upstreamRepos")

// full name -> (branch, traget dir)
Map <String, Tuple2<String, String>> reposToClone = [:]

upstreamRepos.each { upstreamRepoName ->
    if (existsBranch("$forkName/$upstreamRepoName", SOURCE_BRANCH)) {
        reposToClone.put("$forkName/$upstreamRepoName", new Tuple2(SOURCE_BRANCH, new File(BASEDIR, "upstream-repos")))
    }
}

println("Cloning repositories:")
reposToClone.each {
    cloneRepository(it.key, it.value.first, it.value.second)
}

////////////////////
/* Helper methods */
////////////////////
def cloneRepository(String fullRepoName, String branch, File dir) {
    logger.info("Cloning \t$fullRepoName:$branch into $dir")
    // make sure dir exists
    dir.mkdirs()

    def proc = "git clone git://github.com/$fullRepoName --branch $branch --depth 10".execute([], dir)
    proc.consumeProcessOutputStream(System.out)
    proc.consumeProcessErrorStream(System.err)
    proc.waitFor()
}

def logArgs() {
    logger.info("PR link: $PR_LINK")
    logger.info("PR ID: $PR_ID")
    logger.info("Source branch: $SOURCE_BRANCH")
    logger.info("Target branch: $TARGET_BRANCH")
    logger.info("Basedir: $BASEDIR")
}

def boolean existsBranch(String fullRepoName, String branch) {
    logger.debug("Checking existance of $fullRepoName:$branch")
    try {
        github.getRepository(fullRepoName).getBranches().containsKey(branch)
    } catch (FileNotFoundException e) {
        logger.debug("Failed to find repo!", e)
        return false
    }
}

def List<String> getUpstreamRepos(String repo) {
    def repoIdx = ALL_REPOS.findIndexOf { it == repo }
    if (repoIdx == -1) {
        throw new IllegalArgumentException("Repository '$repo' not found in the repository list!")
    }
    ALL_REPOS.subList(0, repoIdx)
}
