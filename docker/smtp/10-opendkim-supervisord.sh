#!/bin/sh
# atlas-opendkim-supervisord — boky/postfix runs OpenDKIM via supervisord on inet:8891.
set -e
MARK="atlas-opendkim-supervisord"
INIT=/etc/init.d/opendkim
grep -q "$MARK" "$INIT" 2>/dev/null && exit 0

rm -f /etc/rc2.d/S01opendkim /etc/rc3.d/S01opendkim /etc/rc4.d/S01opendkim /etc/rc5.d/S01opendkim 2>/dev/null || true

# Insert helper before case statement
sed -i '/^case "\$1" in/i\
# '"$MARK"'\
opendkim_milter_listening() {\
\tnetstat -tln 2>/dev/null | grep -q '"'"'127.0.0.1:8891'"'"';\
}\
' "$INIT"

sed -i '/^  status)$/,/^  \*)$/{ /^[[:space:]]*status \$DAEMON \$NAME/c\
\tif opendkim_milter_listening; then\
\t\tlog_success_msg "$NAME is running (supervisord, inet:8891)";\
\t\texit 0;\
\tfi\
\tstatus $DAEMON $NAME
}' "$INIT"

sed -i '/^start() {/a\
\tif opendkim_milter_listening; then return 0; fi
' "$INIT"

chmod +x "$INIT"
