import groovy.json.JsonSlurper

JOB_PARAMS = [
	CLONE_URL: 'https://github.com/dmaharana/jenkins-test.git',
	BUILD_BRANCH: BUILD_BRANCH,
	DEFAULT_BRANCH: 'master',
	MOD_FILES: [],
]


node('buildnode') {

	cleanWs()
	stage('CID') {
		JOB_PARAMS.MOD_FILES = getModifiedFilesFromPreviousSuccessfulCommit(JOB_PARAMS)
		println ('Modified files: ' + JOB_PARAMS.MOD_FILES.toString())
	}

	stage('Push Files') {
		pushFiles(JOB_PARAMS.MOD_FILES)
	}

}


def getModifiedFilesFromPreviousSuccessfulCommit(JP) {
	
	def changedFiles = []

	scmVars = checkout scm: [$class: 'GitSCM', branches: [[name: "*/${JP.BUILD_BRANCH}"]], extensions: [[$class: 'CloneOption', depth: 1, noTags: false, reference: '', shallow: true]], userRemoteConfigs: [[credentialsId: 'GITHUB_USER_KEY', url: JP.CLONE_URL]]]


	sh 'ls -R'


	println('GIT_COMMIT: '+ scmVars.GIT_COMMIT)
	println('GIT_LOCAL_BRANCH: '+ scmVars.GIT_LOCAL_BRANCH)
	println('GIT_COMMITTER_EMAIL: '+ scmVars.GIT_COMMITTER_EMAIL)
	println('GIT_PREVIOUS_COMMIT: '+ scmVars.GIT_PREVIOUS_COMMIT)
	println('GIT_PREVIOUS_SUCCESSFUL_COMMIT: '+ scmVars.GIT_PREVIOUS_SUCCESSFUL_COMMIT)
    
	def filesMod

    if(scmVars.GIT_PREVIOUS_SUCCESSFUL_COMMIT?.trim()) {
		sh "git fetch origin ${scmVars.GIT_PREVIOUS_SUCCESSFUL_COMMIT}"
		filesMod = sh returnStdout: true, script: "git diff --name-only ${scmVars.GIT_PREVIOUS_SUCCESSFUL_COMMIT}..${scmVars.GIT_COMMIT}"
    } else {
		sh "git fetch origin $DEFAULT_BRANCH"
		filesMod = sh returnStdout: true, script: "git diff --name-only FETCH_HEAD..${scmVars.GIT_COMMIT}"
    }


	if(filesMod?.trim()) {
		filesMod.split("\n").each { fn ->
			// println("Modified file: $fn")
			changedFiles.add(fn)
		}
	} else {
		println("No files")
	}

	return changedFiles
}

def pushFileToTarget(fname) {
	println(getDateTime() + ": Pushing $fname to target")
}


def pushFiles(fileList) {
	def pushQueue = [:]

	fileList.each { fn ->
		pushQueue.put(fn, { pushFileToTarget(fn) })
	}

	parallel pushQueue

}

def generateCrumb() {
    resp = httpRequest acceptType: 'APPLICATION_JSON', authentication: 'USR_PSW', consoleLogResponseBody: true, contentType: 'APPLICATION_JSON', ignoreSslErrors: true, url: 'http://localhost:8080/crumbIssuer/api/json', wrapAsMultipart: false
    println('Status: '+resp.status)
    println('Response: '+resp.content)
    
    def jsonData = new JsonSlurper().parseText(resp.content)
    println('Response: '+jsonData.get('crumb'))
    
    return jsonData.get('crumb')
}

def getModifiedFilesFromChangeSets() {
	def changeLogSets = currentBuild.changeSets
	changedFiles = []
	println('changeSets: '+ changeLogSets.size())
	changeLogSets.each { chgS ->
		def entries = chgS.items
        entries.each { ent ->
            println ("* ${ent.msg} by ${ent.author} \n")
            ent.affectedFiles.each { af ->
            	println('affected File: '+ af.path)
            	changedFiles.add(af.path)
            }
        }
	}

	changedFiles = changedFiles.unique()
	changedFiles.each { fn ->
		println("Modified file: $fn")
	}

}

def getLastSuccessfulCommitId() {
	crumb = generateCrumb()

	jobName = 'Test/job/get-last-successful-commit-2'
	depth = 10
	response = httpRequest acceptType: 'APPLICATION_JSON', authentication: 'USR_PSW', customHeaders: [[maskValue: true, name: 'Jenkins-Crumb', value: crumb]], consoleLogResponseBody: true, contentType: 'APPLICATION_JSON', httpMode: 'POST', ignoreSslErrors: true, url: "http://localhost:8080/job/${jobName}/lastsuccessfulbuild/api/json?tree=actions[lastbuiltrevision[sha1]]&depth=${depth}", wrapAsMultipart: false

	println('Status: '+response.status)
	println('Response: '+response.content)
}


def getDateTime() {
	Date date = new Date()
	String datePart = date.format("dd/MM/yyyy")
	String timePart = date.format("HH:mm:ss")

	return datePart + "_" + timePart
}
