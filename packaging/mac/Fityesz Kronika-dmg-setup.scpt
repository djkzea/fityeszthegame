-- How the disk image opens: the window, the two icons, and the drawn
-- background they stand on.
--
-- The geometry here and packaging/art.py are one design: the arrow on the
-- background is painted between the two positions set below, so moving an
-- icon without moving the arrow leaves it pointing at nothing. The window is
-- 600x400 of content, which is what the background is drawn at.
tell application "Finder"
  set theDisk to a reference to (disks whose URL = "DEPLOY_VOLUME_URL")
  open theDisk

  set theWindow to a reference to (container window of disks whose URL = "DEPLOY_VOLUME_URL")

  set current view of theWindow to icon view
  set toolbar visible of theWindow to false
  set statusbar visible of theWindow to false

  set the bounds of theWindow to {400, 100, 1000, 500}

  set theViewOptions to a reference to the icon view options of theWindow
  set arrangement of theViewOptions to not arranged
  set icon size of theViewOptions to 112
  set text size of theViewOptions to 12
  set label position of theViewOptions to bottom
  set shows item info of theViewOptions to false
  set background picture of theViewOptions to POSIX file "DEPLOY_BG_FILE"

  -- Create alias for install location
  do shell script "(cd 'DEPLOY_VOLUME_PATH' && ln -s 'DEPLOY_INSTALL_LOCATION' 'DEPLOY_INSTALL_LOCATION_DISPLAY_NAME')"

  set allTheFiles to the name of every item of theWindow
  set xpos to 120
  set ypos to 310
  set i to 1
  set j to 0
  repeat with theFile in allTheFiles
    set theFilePath to POSIX path of theFile
    set appFilePath to POSIX path of "/DEPLOY_TARGET"
    if theFilePath ends with "DEPLOY_INSTALL_LOCATION_DISPLAY_NAME" then
      set position of item theFile of theWindow to {450, 205}
    else if theFilePath ends with "DEPLOY_INSTALL_LOCATION" then
      set position of item theFile of theWindow to {450, 205}
    else if theFilePath ends with appFilePath then
      set position of item theFile of theWindow to {150, 205}
    else
      -- anything else the image happens to carry, in a row along the bottom
      set position of item theFile of theWindow to {xpos, ypos}
      set xpos to xpos + 120
      set i to i + 1
      if i is equal to 4 then
        set i to 1
        set xpos to 120
        set ypos to ypos + 110
        set j to j + 1
      end if
    end if
  end repeat

  update theDisk without registering applications
  delay 5
  close (get window of theDisk)
end tell
