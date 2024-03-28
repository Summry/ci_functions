def call(dockerRepoName, imageName) {
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
                            pip install pylint
                            pylint --fail-under=5 --disable import-error ./${dir}/*.py
                            """
                    }
                }
            }

            stage('Security') {
                steps {
                    script {
                        sh 'bandit -r *.py'
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
                        sh "docker build -t ${dockerRepoName}:latest --tag nazzywazzy/${dockerRepoName}:${imageName} ."
                        sh "docker push nazzywazzy/${dockerRepoName}:${imageName}"
                    }
                }
            }
        }
    }
}
