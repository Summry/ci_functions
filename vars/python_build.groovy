def call(service, imageName) {
    pipeline {
        agent any
        environment {
            PATH = "/var/lib/jenkins/.local/bin:$PATH"
        }

        stages {
            stage('Python Lint') {
                steps {
                    script {
                        sh """
                            apt install -y python3-pip
                            pip install pylint
                            pylint --fail-under=5 --disable import-error ./${service}/*.py
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
                        sh "docker build -t ${service}:latest --tag nazzywazzy/${service}:${imageName} ${service}/"
                        sh "docker push nazzywazzy/${service}:${imageName}"
                    }
                }
            }
        }
    }
}
