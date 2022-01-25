buildPlugin()

pipeline {
    agent any


    stages {
        stage('Build') {
            steps {
                // Get some code from a GitHub repository
                git branch: 'ep-rest-api', credentialsId: 'ec062ec6-6daf-40af-9d65-6e9674027e9a', url: 'https://github.com/thabok/btc-embeddedplatform-plugin.git'
            }
        }
    }
}
