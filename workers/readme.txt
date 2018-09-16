$  sbt run java -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false
$  /opt/akka-2.4.18/bin/akka-cluster 0.0.0.0 9999 cluster-status
$  /opt/akka-2.4.18/bin/akka-cluster 0.0.0.0 9999 down akka.tcp://LeomaxClusterSystem@0.0.0.0:49603
$  sbt aspectj-runner:run -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false
$  java -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -jar backend-workers-assembly-1.0.jar


export WORKER_PORT=2771
export WORKER_PORT=2772

$  sbt assembly
$  cp target/scala-2.12/backend-workers-assembly-1.0.jar ./docker/dockerfile1/
$  docker build -t worker1_f ./docker/dockerfile1/.
$  docker build -t worker2_f ./docker/dockerfile2/.
$  docker build -t worker0_f ./docker/dockerfile0/.
$  docker run -e TZ=Europe/Moscow --name worker1_f --net=host -d worker1_f:latest
$  docker run -e TZ=Europe/Moscow --name worker2_f --net=host -d worker2_f:latest
$  docker run -e TZ=Europe/Moscow --name worker0_f --net=host -d worker0_f:latest



docker run -e TZ=Europe/Moscow --name worker1_f --net=host -d worker1:latest
docker run -e TZ=Europe/Moscow --name worker2_f --net=host -d worker2:latest
docker run -e TZ=Europe/Moscow --name rest_f --net=host -d rest:latest