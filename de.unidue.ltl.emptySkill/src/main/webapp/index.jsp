<html>
<body>
<h2 align="center" style="font-family: Arial, Verdana; font-size:500%">Du hast gesagt: </h2> <br><br><br><br>

<p align=center style="font-family: Arial, Verdana; font-size:500%">
<%
    String a = com.amazon.customskill.AlexaSkillSpeechlet.userRequest;
	if (a==null){
	    a = "--init--";
	}
	out.println("  " + a);
	response.setIntHeader("Refresh", 1);
%> 


</p>
</body>
</html>
