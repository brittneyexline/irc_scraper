#!/bin/sh
#
############################################################################
#
# Andrew G. West - safe_browsing_cron.sh - Simple shell call to the Google
# Safe Browsing update script. It is anticipated this script will be
# called by 'cron' so that updates are run every 30 minutes.
#
############################################################################

JAVABASE=/home/westand/Safe_Browsing

    # Just run the update class
java -cp $JAVABASE update_list.update_list
exit 0
