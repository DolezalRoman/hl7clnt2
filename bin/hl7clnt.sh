#! /usr/bin/env sh

# $Id: hl7clnt.sh,v 1.10 2020/02/26 14:44:03 amis Exp $
# $Log: hl7clnt.sh,v $
# Revision 1.10  2020/02/26 14:44:03  amis
# Migrated to OpenJDK 8.
#
# Revision 1.9  2017/05/02 13:17:15  raska
# doplnen chybejici bin/hl7clnt.sh
#
# Revision 1.4  2005/10/16 17:01:25  raska
# Oprava v nazvu funkce pro mazani databaze.
#
# Revision 1.3  2005/10/10 10:37:05  raska
# Zmeneno jmeno funkce stop na stopit.
#
# Revision 1.2  2005/10/10 10:14:42  raska
# Doplneno do projektu.
#
# Revision 1.1  2005/10/09 16:41:27  raska
# Zavedeni do nove repository.
#
# Revision 1.2  2005/07/20 10:23:00  raska
# Doplneno nastaveni environment promennych LANG a LC_ALL. Je to potreba pro pripojeni na ceskou databazi AMIS*H.
#
# Revision 1.1  2005/07/15 09:24:34  raska
# Prejmenovan starovaci skript.
#
# Revision 1.2  2005/06/02 18:27:11  raska
# Zmenen typ souboru v CVS.
#

HL7CLNT_HOME=/opt/hl7clnt

###LANG=cs_cz.UTF-8
LANG=cs_CZ.utf8
LC_ALL=$LANG

JAVA_PARAMS="-Xss16m -Xmx128m"
CLASSES="-cp $( echo $( find $HL7CLNT_HOME/lib -name '*.jar' ) | sed 's/ /:/g' )"
PROGRAM="cz.i.amish.hl7clnt2.client.HL7Clnt"
PARAMS=$([[ -f /opt/hl7clnt/var/conf/HL7Clnt.property ]] && echo "/opt/hl7clnt/var/conf/HL7Clnt.property")
CLEANDB_PROG="cz.i.amish.hl7clnt2.client.HL7DbCleaner"
PID_FILE=var/hl7clnt.pid

start () {
        java $JAVA_PARAMS $CLASSES $PROGRAM $PARAMS &
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
        java $JAVA_PARAMS $CLASSES $CLEANDB_PROG $PARAMS &
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
