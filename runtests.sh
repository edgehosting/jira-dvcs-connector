#!/bin/bash -x
## copied from jira-development-status-plugin
# a script to run the tests on bamboo build
# for local execution, do something like this:
# mvn -B clean verify -Dselenium.browser.path=/Applications/Firefox.app/Contents/MacOS/firefox -Djava.awt.headless=true -Dmaven.test.failure.ignore=true -PrunTestSuite -DtestGroups=QUnit

OPTS="$*"



vncdisplay=""

killvnc() {
    if ! test -z "${vncdisplay}"; then
        echo stopping vncserver on $DISPLAY
        vncserver -kill $vncdisplay >/dev/null 2>&1
    fi
}



displayEnv() {

        echo "---------------------------------------------"
        echo "Displaying Environment Variables"
        echo "---------------------------------------------"
        env
        echo "---------------------------------------------"
}

echo starting vncserver
vncdisplay=$(vncserver 2>&1 | perl -ne '/^New .* desktop is (.*)$/ && print"$1\n"')
if [[ -z "$vncdisplay" ]]; then
     echo "failed to create a vncserver or get its display identifier"
     exit 2
fi

export DISPLAY=$vncdisplay
echo vncserver started on $DISPLAY

displayEnv

# Move the mouse pointer out of the way
# echo Moving mouse pointer to 10 10.
# xwarppointer abspos 10 10

#Make sure the VNC server is killed always. Why wont you just die!
trap killvnc INT TERM EXIT

if [ -f "${M2_HOME}/bin/mvn" ]; then
  MVN=${M2_HOME}/bin/mvn
else
  MVN=mvn
fi

echo Starting $MVN $OPTS
$MVN $OPTS
MVN_STATUS=$?

exit $MVN_STATUS
