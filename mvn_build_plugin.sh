LOCAL_JENKINS_STACK=/Users/nathand/.jenkins

echo ------------------------------------------------------------------------
echo [1/3] Build btc-embeddedplatform plugin
echo ------------------------------------------------------------------------
mvn clean package -DskipTests=true

echo ------------------------------------------------------------------------
echo [2/3] Copy btc-embeddedplatform.hpi into Jenkins controller build folder
echo ------------------------------------------------------------------------
rm -rf $LOCAL_JENKINS_STACK/plugins/btc-embeddedplatform*
cp target/btc-embeddedplatform.hpi $LOCAL_JENKINS_STACK/plugins
cd $LOCAL_JENKINS_STACK

echo ------------------------------------------------------------------------
echo [3/3] Restarting the docker-compose stack
echo ------------------------------------------------------------------------
docker-compose restart
jenkins-lts restart

echo done
