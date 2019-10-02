pipeline {
    agent { label 'agent' }
    parameters {
        booleanParam(name: 'TEST', defaultValue: true, description: '')
        booleanParam(name: 'BUILD', defaultValue: true, description: '')
        booleanParam(name: 'DOCKER_DEPLOY', defaultValue: true, description: 'Build and push new docker image to docker registry')
        //booleanParam(name: 'SWARM_DEPLOY', defaultValue: false, description: 'Deploy Service to Swarm cluster')
    }

    tools {
        maven 'M3'
        jdk 'jdk8u152'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '2'))
        disableConcurrentBuilds()
    }

    stages {
        stage('Initialize') {
            steps {
                sh '''
                    echo "PATH = ${PATH}"
                    echo "M2_HOME = ${M2_HOME}"
                '''
                sh "mvn -version"
            }
        }

        stage('Unit Tests') {
            when {
                expression {
                    params.TEST
                }
            }
            steps {
                maven_build("clean test")
            }

            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/TEST-*.xml'
                    jacoco(execPattern: '**/*.exec')
                }
            }
        }

        stage('Acceptance Tests') {
            when {
                expression {
                    params.TEST
                }
            }
            steps {
                maven_build("clean verify")
            }

            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/failsafe-reports/TEST-*.xml'
                }
            }
        }

        stage('Build') {
            when {
                expression {
                    params.BUILD
                }
            }
            steps {
                maven_build("-DskipTests -DskipITs clean install")
            }
        }

        stage('Push Docker Image') {
            when {
                expression {
                    params.DOCKER_DEPLOY
                }
            }
            steps {
                withDockerRegistry([credentialsId: 'docker-registry', url: 'https://registry.sparkworks.net']) {
                    script {

                        def imageMapper = docker.build("registry.sparkworks.net/ipn-mapper:1.0",
								"--build-arg JAR_FILE=./target/ipn-mapper-1.0.0-SNAPSHOT.jar ./ipn-mapper")
						imageMapper.push()
						imageMapper.push('latest')
                    }
                }
            }
        }
       stage('Deploy Docker Image') {
           agent { label 'manager' }
           when {
               expression {
                   params.SWARM_DEPLOY
               }
           }
           steps {
               withCredentials([usernamePassword(credentialsId: 'docker-registry', passwordVariable: 'PASS', usernameVariable: 'USER')]) {
                   sh "docker login -u=$USER -p=$PASS registry.sparkworks.net"
                   sh "docker service update --with-registry-auth --force --image registry.sparkworks.net/ipn-mapper:1.0 smartwork_swork-ipn-mapper"
               }
           }
       }
    }

    post {
        always {
            notifyBuild(currentBuild.result)
            deleteDir()
        }
    }
}


def maven_build(lifecycle) {
    configFileProvider(
            [configFile(fileId: 'sparks-setting.xml', variable: 'MAVEN_SETTINGS')]) {
        sh """mvn -s $MAVEN_SETTINGS ${lifecycle}"""
    }
}

def notifyBuild(String buildStatus = 'STARTED') {
    // build status of null means successful
    buildStatus = buildStatus ?: 'SUCCESS'

    // Default values
    def colorName = 'RED'
    def colorCode = '#FF0000'
    def changes = changeLogs()
    def subject = "${env.JOB_NAME} - #${env.BUILD_NUMBER} Status:  ${buildStatus} \nChanges:  ${changes}"
    def summary = "${subject} (<${env.RUN_DISPLAY_URL}|Open>)"
    def details = """<p>STARTED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
    <p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&QUOT;</p>"""

    // Override default values based on build status
    if (buildStatus == 'STARTED') {
        color = 'YELLOW'
        colorCode = '#FFFF00'
    } else if (buildStatus == 'SUCCESS') {
        color = 'GREEN'
        colorCode = '#00FF00'
    } else {
        color = 'RED'
        colorCode = '#FF0000'
    }

    // Send notifications
        mattermostSend color: colorCode , message: summary
}

@NonCPS
def changeLogs() {
    String msg = ""
    def changeLogSets = currentBuild.changeSets

    for (int i = 0; i < changeLogSets.size(); i++) {
        def entries = changeLogSets[i].items
        for (int j = 0; j < entries.length; j++) {
            def entry = entries[j]
            lastId = entry.commitId
            msg = msg + "${lastId}" + ": " + "`${entry.commitId.take(7)}`  *${entry.msg}* _by ${entry.author}_ \n"
        }
    }
    return msg
}
