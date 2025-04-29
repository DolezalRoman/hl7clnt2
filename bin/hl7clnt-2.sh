#! /usr/bin/env sh

HL7CLNT_HOME=/AMIS_H/hl7clnt

# Nastavuje se podle moznosti hostitelskeho systemu 
# dodano LT.
. /AMIS_H/sys/etc/amis_h.config
export JAVA_HOME=/opt/java6
export PATH=$PATH:/opt/java6/bin

###LANG=cs_cz.UTF-8
LANG=cs_CZ.utf8
LC_ALL=$LANG

JAVA_PARAMS="-Xss16m -Xmx128m"
CLASSES="-cp $( echo $( find $HL7CLNT_HOME/lib -name '*.jar' ) | sed 's/ /:/g' )"
PROGRAM="cz.i.amish.hl7clnt2.client.HL7Clnt conf/HL7Clnt-2.property"
CLEANDB_PROG="cz.i.amish.hl7clnt2.client.HL7DbCleaner"
PID_FILE=var/hl7clnt-2.pid

start () {
        java $JAVA_PARAMS $CLASSES $PROGRAM &
        echo $! >$PID_FILE
}

stopit () {
        kill $(cat $PID_FILE)
        rm $PID_FILE
}

check () {
        ps -f -p $(cat $PID_FILE 2>/dev/null) 2>/dev/null | grep -q "java $JAVA_PARAMS"
}

cleandb () {
        java $JAVA_PARAMS $CLASSES $CLEANDB_PROG &
}

cd $HL7CLNT_HOME

case "$1" in
        "start")
                start
                ;;
        "stop")
                stopit
                ;;
        "restart")
                stopit
                start
                ;;
        "cron")
                check || start
                ;;
        "cleandb")
                cleandb
                ;;
        "status")
                check && echo "HL7Clnt is running." || echo "HL7Clnt is stopped."
                ;;
        "version")
                # java $CLASSES $VERSION
                ;;
        *)
                echo "Usage: $(basename $0) start|stop|restart|cron|cleandb|status|version"
                exit 1
                ;;
esac
	