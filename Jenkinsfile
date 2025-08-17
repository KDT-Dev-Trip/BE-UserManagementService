// Jenkinsfile

pipeline {
    // 파이프라인을 실행할 Jenkins Agent(실행 환경)를 지정. 'any'는 어떤 가용한 Agent든 사용하겠다는 의미
    agent any

    // 파이프라인 전체에서 사용할 환경 변수를 정의
    environment {
        // Docker Hub 인증 정보 ID (Jenkins의 Credentials에 미리 저장되어 있어야 함)
        DOCKER_CREDENTIALS_ID = 'dockerhub-credentials'
        // Docker Hub 사용자 이름
        DOCKER_USERNAME = 'your-dockerhub-username'
        // 생성할 Docker 이미지의 이름
        DOCKER_IMAGE_NAME = 'user-management-service'
        // Kubernetes 설정 파일을 모아둔 Git 리포지토리 주소
        CONFIG_REPO_URL = 'https://github.com/your-username/DevTrip-k8s-manifests.git'
    }

    // 파이프라인의 각 단계를 정의
    stages {
        // 1단계
        stage('Build') {
            steps {
                // gradlew 스크립트에 실행 권한을 부여
                sh 'chmod +x ./gradlew'
                // Gradle을 사용하여 프로젝트를 빌드 (테스트는 이 단계에서 제외)
                sh './gradlew build -x test'
            }
        }
        // 2단계
        stage('Test') {
            steps {
                // Gradle을 사용하여 단위 테스트 및 통합 테스트를 실행
                sh './gradlew test'
            }
        }
        // 3단계
        stage('Build Docker Image') {
            steps {
                // Dockerfile을 사용하여 Docker 이미지를 빌드
                // -t 옵션으로 '사용자이름/이미지이름:빌드번호' 형식의 태그를 붙임
                sh "docker build -t ${DOCKER_USERNAME}/${DOCKER_IMAGE_NAME}:${BUILD_NUMBER} ."
            }
        }
        // 4단계
        stage('Push Docker Image') {
            steps {
                // Jenkins Credentials에 저장된 인증 정보를 사용하여 Docker Hub에 로그인
                withCredentials([usernamePassword(credentialsId: DOCKER_CREDENTIALS_ID, usernameVariable: 'DOCKER_USERNAME_VAR', passwordVariable: 'DOCKER_PASSWORD_VAR')]) {
                    // Docker Hub에 로그인하는 명령어
                    sh "echo ${DOCKER_PASSWORD_VAR} | docker login -u ${DOCKER_USERNAME_VAR} --password-stdin"
                }
                // 빌드한 Docker 이미지를 Docker Hub로 푸시(업로드)
                sh "docker push ${DOCKER_USERNAME}/${DOCKER_IMAGE_NAME}:${BUILD_NUMBER}"
            }
        }
        stage('Update K8s Manifest') {
            steps {
                 // 별도의 작업 공간('config-repo')에서 Git 설정 리포지토리를 클론
                 dir('config-repo') {
                     git url: CONFIG_REPO_URL, branch: 'main'

                     // sed 명령어를 사용하여 deployment.yaml 파일의 이미지 태그를 현재 빌드 번호로 변경
                     sh "sed -i 's|image:.*|image: ${DOCKER_USERNAME}/${DOCKER_IMAGE_NAME}:${BUILD_NUMBER}|g' services/user-service/deployment.yaml"

                     // 변경된 내용을 Git에 커밋
                     sh 'git config --global user.email "jenkins@example.com"'
                     sh 'git config --global user.name "Jenkins CI"'
                     sh 'git add .'
                     sh "git commit -m 'Update user-service to version ${BUILD_NUMBER}'"

                     // 변경된 내용을 Git 리포지토리에 푸시
                     sh 'git push origin main'
                 }
            }
        }
    }
    // 파이프라인의 모든 단계가 성공/실패 여부와 상관없이 끝난 후에 항상 실행
    post {
        always {
            // Docker Hub에서 로그아웃하여 보안을 유지
            sh 'docker logout'
            // Gradle 빌드 과정에서 생성된 임시 파일들을 정리
            sh './gradlew clean'
        }
    }
}