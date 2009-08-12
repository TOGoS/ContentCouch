CCOUCH_PROJ_DIR=`dirname "$0"`
CCOUCH_MAIN_REPO="$CCOUCH_PROJ_DIR"/junk-repo

java -cp "$CCOUCH_PROJ_DIR"/web/WEB-INF/classes:"$CCOUCH_PROJ_DIR"/ext-lib/togos.mf-2009.08.07b.jar contentcouch.app.ContentCouchCommand -repo "$CCOUCH_MAIN_REPO" "$@"
