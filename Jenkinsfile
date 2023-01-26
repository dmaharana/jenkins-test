node('master') {
    clean_ws()

    stage('Prepare') {
        echo 'Preparing'
        prepare()
    }
    
    stage('Test') {
        echo 'Testing'
    }
    
    clean_ws()
}

def clean_ws() {
    cleanWs(cleanWhenNotBuilt: false,
                    deleteDirs: true,
                    disableDeferredWipeout: true,
                    notFailBuild: true)
}

def prepare() {
	repoName = 'jenkins-test'
	text = get_date_time() + ': wrote from jenkins pipeline'

	// set_host_key_verify_false()
	set_known_hosts('github.com')

	// withCredentials([file(credentialsId: 'git-private-key', variable: 'private_key')]) {
	withCredentials([sshUserPrivateKey(credentialsId: 'git-ssh-user-key', keyFileVariable: 'private_key', usernameVariable: 'USER')]) {
		// Refer the ssh private key
		//  corresponding ssh public should be configured to access repo 
		sh "git clone -c \"core.sshCommand=ssh -i \$private_key\" git@github.com:dmaharana/jenkins-test.git -b $BUILD_BRANCH"

		// Any further git operation has to be within this cred block
		dir(repoName) {
			sh "echo $text >> test.txt"
			sh 'git config user.name "Mona Lisa" && git config user.email mona_lisa@example.com'
			sh 'git commit -am"commit from pipeline" && git push'
		}
	}

}

def set_known_hosts(base_url) {
	sh "ssh-keygen -F $base_url || mkdir -p ~/.ssh/ && ssh-keyscan $base_url >>~/.ssh/known_hosts"
}

def set_host_key_verify_false() {
	// set global configuration not to prompt host key verification
	sh "git config --global core.sshCommand 'ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no'"
}

def get_date_time() {
	Date date = new Date()
	String datePart = date.format("dd/MM/yyyy")
	String timePart = date.format("HH:mm:ss")

	return datePart + "_" + timePart
}
