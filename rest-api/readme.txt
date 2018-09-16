
$  sbt assembly
$  cp target/scala-2.12/backend-rest-api-assembly-1.0.jar ./
$  docker build -t rest_f .
$  docker run -e TZ=Europe/Moscow --name rest_f --net=host -v /shared/valumes/file_io:/home -d rest_f:latest