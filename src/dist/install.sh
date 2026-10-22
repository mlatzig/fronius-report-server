#!/bin/sh
#
# Installs the report server as a systemd service.
# Run it as root from the deployment directory:
#
#     sudo ./install.sh        asks for user, directory and java
#     sudo ./install.sh -y     takes the suggested values without asking
#
set -e

UNIT=fronius_report_server.service
JAR=FroniusReportServer.jar
HERE=$(cd "$(dirname "$0")" && pwd)

ASK=1
if [ "$1" = "-y" ]; then
	ASK=0
fi

if [ "$(id -u)" -ne 0 ]; then
	echo "Please run as root: sudo ./install.sh" >&2
	exit 1
fi

# ask VARIABLE PROMPT DEFAULT - reads a value, Enter keeps the default
ask() {
	value=$3
	if [ $ASK -eq 1 ]; then
		printf '%-18s[%s]: ' "$2" "$3"
		read -r answer
		if [ -n "$answer" ]; then
			value=$answer
		fi
	fi
	eval "$1=\$value"
}

ask SVC_USER "Service user" "${SUDO_USER:-root}"
ask SVC_DIR "Install directory" "$HERE"
ask SVC_JAVA "Java" "$(command -v java || echo /usr/bin/java)"

# --- checks ----------------------------------------------------------------
if ! id "$SVC_USER" >/dev/null 2>&1; then
	echo "User not found: $SVC_USER" >&2
	exit 1
fi
if [ ! -f "$SVC_DIR/$JAR" ]; then
	echo "$JAR not found in $SVC_DIR - unzip the distribution there first." >&2
	exit 1
fi
if [ ! -x "$SVC_JAVA" ]; then
	echo "Java not found or not executable: $SVC_JAVA" >&2
	exit 1
fi
if [ ! -f "$HERE/$UNIT" ]; then
	echo "Unit template not found: $HERE/$UNIT" >&2
	exit 1
fi

echo
echo "Installing $UNIT:"
echo "  User=$SVC_USER"
echo "  WorkingDirectory=$SVC_DIR"
echo "  JAVA=$SVC_JAVA"
if [ $ASK -eq 1 ]; then
	printf 'Continue? [Y/n] '
	read -r answer
	case "$answer" in
	[nN]*)
		echo "Aborted."
		exit 1
		;;
	esac
fi

# --- first installation: settings of this installation ---------------------
if [ ! -f "$SVC_DIR/local.properties" ] && [ -f "$SVC_DIR/local.properties.example" ]; then
	cp "$SVC_DIR/local.properties.example" "$SVC_DIR/local.properties"
	chown "$SVC_USER" "$SVC_DIR/local.properties"
	echo
	echo "Created $SVC_DIR/local.properties - set fronius.csv-dir there."
	echo "Until then the server shows a sample month."
fi

# --- install the unit ------------------------------------------------------
sed -e "s|@USER@|$SVC_USER|g" -e "s|@DIR@|$SVC_DIR|g" -e "s|@JAVA@|$SVC_JAVA|g" \
	"$HERE/$UNIT" > "/etc/systemd/system/$UNIT"
chmod 644 "/etc/systemd/system/$UNIT"
systemctl daemon-reload
systemctl enable "$UNIT"
systemctl restart "$UNIT"

echo
systemctl --no-pager --lines=10 status "$UNIT" || true
