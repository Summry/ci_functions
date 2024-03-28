def call(service, imageName) {
    pipeline {
        agent any
        environment {
            PATH = "/var/lib/jenkins/.local/bin:$PATH"
        }
        stages {
            stage('Build') {
                steps {
                    script {
                        sh 'python3 -m venv venv'
                        sh '. venv/bin/activate'
                        sh 'pip install -r requirements.txt'
                    }
                }
            }
            stage('Python Lint') {
                steps {
                    script {
                        sh """
                            pip install pylint
                            pylint --fail-under=5 ./${service}/*.py
                            """
                    }
                }
            }
            stage('Security') {
                steps {
                    script {
                        sh """
                            pip install bandit
                            bandit -r ./${service}
                            """
                    }
                }
            }
            stage('Package') {
                when {
                    expression { env.GIT_BRANCH == 'origin/main' }
                }
                steps {
                    withCredentials([string(credentialsId: 'DockerHub', variable: 'TOKEN')]) {
                        sh "docker login -u 'nazzywazzy' -p '$TOKEN' docker.io"
                        sh "docker build -t ${service}:latest --tag nazzywazzy/${service}:${imageName} ./${service}"
                        sh "docker push nazzywazzy/${service}:${imageName}"
                    }
                }
            }
            stage('Deploy') {
                steps {
                    sshagent(credentials : ['ssh-key']) {
                        sh 'ssh -t -t azureuser@52.160.84.127 -o StrictHostKeyChecking=no "cd acit3855-project && docker compose up --build -d"'
                    }
                }
            }
        }
        post {
            always {
                script {
                    sh 'unset PATH'
                }
            }
        }
    }
}
