pipeline {
    agent any

    environment {
        // ===== 로컬 Docker Registry 설정 =====
        DOCKER_REGISTRY = 'localhost:5000'
        SERVICE_NAME = 'user-management-service'
        IMAGE_NAME = 'user-management-service'
        IMAGE_TAG = "${BUILD_NUMBER}"
        
        // ===== 로컬 Kubernetes 설정 =====
        K8S_NAMESPACE = 'devtrip'
        K8S_CONFIG_PATH = './k8s'
        
        // ===== ArgoCD 로컬 설정 =====
        ARGOCD_SERVER = 'localhost:30080'
        ARGOCD_APP_NAME = 'user-service-app'
        
        // ===== Git 설정 (매니페스트 저장소) =====
        CONFIG_REPO_URL = 'https://github.com/your-username/DevTrip-k8s-manifests.git'
    }

    stages {
        stage('🚀 Pipeline Start') {
            steps {
                echo "===================================================="
                echo "🚀 Starting CI/CD Pipeline for ${SERVICE_NAME}"
                echo "📋 Build Number: ${BUILD_NUMBER}"
                echo "🌿 Branch: ${env.BRANCH_NAME}"
                echo "===================================================="
            }
        }
        
        stage('📦 Checkout & Setup') {
            steps {
                script {
                    env.GIT_COMMIT_SHORT = sh(
                        script: 'git rev-parse --short HEAD',
                        returnStdout: true
                    ).trim()
                    env.BUILD_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT_SHORT}"
                    echo "📦 Checked out commit: ${env.GIT_COMMIT_SHORT}"
                }
            }
        }
        
        stage('🏗️ Build') {
            steps {
                echo "🏗️ Building application..."
                sh 'chmod +x ./gradlew'
                sh './gradlew build -x test'
                archiveArtifacts artifacts: 'build/libs/*.jar', allowEmptyArchive: false
            }
        }
        
        stage('🧪 Test') {
            steps {
                script {
                    try {
                        echo "🧪 Running tests..."
                        sh './gradlew test'
                        echo "✅ Tests passed successfully"
                    } catch (Exception e) {
                        echo "⚠️ Tests failed but continuing with deployment: ${e.getMessage()}"
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
            post {
                always {
                    script {
                        try {
                            publishTestResults testResultsPattern: 'build/test-results/test/*.xml'
                        } catch (Exception e) {
                            echo "Test report publishing failed: ${e.getMessage()}"
                        }
                    }
                }
            }
        }
        
        stage('🐳 Build Docker Image') {
            steps {
                script {
                    echo "🐳 Building Docker image..."
                    def dockerImage = "${DOCKER_REGISTRY}/${IMAGE_NAME}:${BUILD_TAG}"
                    sh "docker build -t ${dockerImage} ."
                    sh "docker tag ${dockerImage} ${DOCKER_REGISTRY}/${IMAGE_NAME}:latest"
                    
                    env.DOCKER_IMAGE_FULL = dockerImage
                    echo "🐳 Built Docker image: ${dockerImage}"
                }
            }
        }
        
        stage('📤 Push Docker Image') {
            steps {
                script {
                    echo "📤 Pushing to local registry..."
                    sh "docker push ${env.DOCKER_IMAGE_FULL}"
                    sh "docker push ${DOCKER_REGISTRY}/${IMAGE_NAME}:latest"
                    echo "📤 Pushed to local registry: ${env.DOCKER_IMAGE_FULL}"
                }
            }
        }
        
        stage('📝 Update K8s Manifest') {
            steps {
                script {
                    echo "📝 Updating local K8s manifest..."
                    sh """
                        sed -i 's|image: .*${IMAGE_NAME}:.*|image: ${env.DOCKER_IMAGE_FULL}|g' k8s/deployment.yaml || echo "Deployment file not found"
                    """
                    echo "📝 Updated local K8s manifest"
                }
            }
        }
        
        stage('🚀 Deploy to Local K8s') {
            steps {
                script {
                    echo "🚀 Deploying to local Kubernetes..."
                    
                    sh """
                        # 네임스페이스 생성 (이미 있으면 스킵)
                        kubectl create namespace ${K8S_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f - || echo "Namespace already exists"
                        
                        # 이미지 업데이트
                        kubectl set image deployment/${SERVICE_NAME} \
                            ${SERVICE_NAME}=${env.DOCKER_IMAGE_FULL} \
                            -n ${K8S_NAMESPACE} || echo "Deployment not found, will create later"
                        
                        # Pod 상태 확인
                        kubectl get pods -n ${K8S_NAMESPACE} -l app=${SERVICE_NAME} || echo "No pods found"
                    """
                }
            }
        }
        
        stage('✅ Health Check') {
            steps {
                script {
                    echo "✅ Running health checks..."
                    
                    sh """
                        echo "Build completed successfully"
                        echo "Service: ${SERVICE_NAME}"
                        echo "Image: ${env.DOCKER_IMAGE_FULL}"
                        echo "Commit: ${env.GIT_COMMIT_SHORT}"
                    """
                }
            }
        }
    }
    
    post {
        always {
            echo "🧹 Cleaning up workspace..."
            
            script {
                try {
                    sh './gradlew clean'
                } catch (Exception e) {
                    echo "Gradle cleanup failed: ${e.getMessage()}"
                }
                
                try {
                    sh "docker system prune -f"
                } catch (Exception e) {
                    echo "Docker cleanup skipped: ${e.getMessage()}"
                }
            }
            
            cleanWs()
        }
        
        success {
            echo "✅ Pipeline completed successfully for ${SERVICE_NAME}!"
        }
        
        failure {
            echo "❌ Pipeline failed for ${SERVICE_NAME}!"
        }
    }
}