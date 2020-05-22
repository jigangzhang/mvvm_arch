#!/bin/sh

#Ask some questions and collect the answer

dialog --title "Questionnaire" --msgbox "Welcome to my simple survey" 9 18

dialog --title "Confirm" --yesno "Are you willing to take part?" 9 18
if [ $? != 0 ];then
	dialog --infobox "Thank you anyway" 5 20
	sleep 2
	dialog --clear
	exit 0
fi

dialog --title "Questionnaire" --inputbox "Please enter you name" 9 30 2>_1.txt
q_name=$(cat _1.txt)
echo $q_name

dialog --menu "$q_name, what music do you like best?" 15 30 4 1 Classical 2 Jazz 3 Country 4 Other 2>_1.txt
q_music=$(cat _1.txt)
echo $q_music

if [ "$q_music" = 1 ];then
	dialog --title "Likes Classical" --msgbox "Good choice!" 12 25
else
	dialog --title "Doesn't like Classical" --msgbox "Shame" 12 25
fi

sleep 2
dialog --clear
exit 0
