CCOUCH_PROJ_DIR=`dirname "$0"`

java -jar "$CCOUCH_PROJ_DIR"/ext-lib/winstone-0.9.10.jar \
    --commonLibFolder="$CCOUCH_PROJ_DIR"/ext-lib \
    --useServletReloading=true \
    "$CCOUCH_PROJ_DIR"/web
