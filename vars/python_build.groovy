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
                        sh 'sudo apt-get update -y && sudo apt-get install -y python3.8-venv python3-pip'
                        sh 'python3 -m venv venv'
                        sh '. venv/bin/activate'
                        sh 'pip install -r requirements.txt'
                    }
                }
            }
            stage('Python Lint') {
                steps {
                    script {
                        sh 'pip install pylint'
                        sh 'pylint --fail-under=5 ./${service}/**.py'
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
