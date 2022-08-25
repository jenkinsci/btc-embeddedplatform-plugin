LOCAL_JENKINS_STACK=/Users/thabok/Bitbucket/local-jenkins-stack

echo ------------------------------------------------------------------------
echo [1/3] Build btc-embeddedplatform plugin
echo ------------------------------------------------------------------------
mvn clean package -DskipTests=true

echo ------------------------------------------------------------------------
echo [2/3] Copy btc-embeddedplatform.hpi into Jenkins controller build folder
echo ------------------------------------------------------------------------
rm -rf $LOCAL_JENKINS_STACK/jenkins_home/plugins/btc-embeddedplatform*
cp target/btc-embeddedplatform.hpi $LOCAL_JENKINS_STACK/jenkins_home/plugins
cd $LOCAL_JENKINS_STACK

echo ------------------------------------------------------------------------
echo [3/3] Restarting the docker-compose stack
echo ------------------------------------------------------------------------
docker-compose restart

echo done
