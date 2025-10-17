 echo "### build frontend ###"
 cd ruoyi-ui
 npm install
 npm run build:prod

 echo "### build backend ###"
 cd ..
 mvn clean package -DskipTests

 echo "### build docker image ###"
 docker build -t ruoyi-ui -f Dockerfile-ui .
 docker build -t ruoyi-admin -f Dockerfile-admin .

