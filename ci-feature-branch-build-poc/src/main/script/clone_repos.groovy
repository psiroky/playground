import groovy.transform.Field
import org.kohsuke.github.GitHub
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// TODO: get the list of KIE repos from the repository-list.txt
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
        "kie-docs",
        "kie-wb-distributions",
        "droolsjbpm-build-distribution",
        "kie-eap-modules"
]

@Field
static final Logger logger = LoggerFactory.getLogger(getClass())

@Field
static final String ACCESS_TOKEN = System.getProperty("accessToken")

@Field
static final String USER_OR_ORG = System.getProperty("userOrOrg")

@Field
static final String BRANCH = System.getProperty("branch")

@Field
static final String DEFAULT_BRANCH = "master"

@Field
static final String START_FROM = System.getProperty("startFrom")

@Field
static final File BASEDIR = new File(System.getProperty("basedir"))

@Field
GitHub github = GitHub.connectUsingOAuth(ACCESS_TOKEN)

logArgs()

Map<String, String> reposToClone = gatherReposToClone()
reposToClone.each {
    logger.info("Cloning \t$it.key/$it.value")

    def proc = "git clone git://www.github.com/$it.key --branch $it.value".execute([], BASEDIR)
    proc.consumeProcessOutputStream(System.out)
    proc.consumeProcessErrorStream(System.err)
    proc.waitFor()
}

def logArgs() {
    logger.info("User or org: $USER_OR_ORG")
    logger.info("Branch: $BRANCH")
    logger.info("Start from: $START_FROM")
    logger.info("Basedir: $BASEDIR")
}

def gatherReposToClone() {
    Map<String, String> reposToClone = [:]
    getReposFrom(START_FROM).each { repoName ->
        def defaultRepo = getDefaultOrgUnitForRepo(repoName) + "/$repoName"
        def fork = "$USER_OR_ORG/$repoName"
        // 1) Check the fork + specified branch
        // 2) Check the default repo + specified branch
        // 3) If none above exists, use default repo + default (master) branch

        if (existsBranch(fork, BRANCH)) {
            reposToClone.put(fork, BRANCH)
        } else if (existsBranch("$defaultRepo", BRANCH)) {
            reposToClone.put(defaultRepo, BRANCH)
        } else {
            reposToClone.put(defaultRepo, DEFAULT_BRANCH)
        }
    }
    reposToClone
}

def boolean existsBranch(String repo, String branch) {
    logger.debug("Checking existance of $repo/$branch")
    try {
        github.getRepository(repo).getBranches().containsKey(branch)
    } catch (FileNotFoundException e) {
        logger.debug("Failed to find repo!", e)
        return false
    }
}


def List<String> getReposFrom(String startRepo) {
    def startRepoIdx = ALL_REPOS.findIndexOf { repo -> repo == startRepo }
    if (startRepoIdx == -1) {
        throw new IllegalArgumentException("Start repository '$startRepo' not recognized!")
    }
    ALL_REPOS.subList(startRepoIdx, ALL_REPOS.size() - 1)
}

def String getDefaultOrgUnitForRepo(String repo) {
    if (repo.startsWith("uberfire")) {
        return "uberfire"
    } else if (repo.startsWith("dashbuilder")) {
        return "dashbuilder"
    } else {
        return "droolsjbpm"
    }
}
