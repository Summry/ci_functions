def call(dockerRepoName, imageName) {
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
                        sh 'pip install -r requirements.txt --break-system-packages'
                        sh 'pip install --upgrade flask --break-system-packages'
                        sh 'pip install coverage --break-system-packages'
                    }
                }
            }

            // Newly added stage in Lab 6
            stage('Python Lint') {
                steps {
                    script {
                        sh 'pylint --fail-under=5 *.py'
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
            
            stage('Zip Artifacts') {
                steps {
                    sh 'zip app.zip *.py'
                    archiveArtifacts artifacts: 'app.zip', onlyIfSuccessful: true
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
