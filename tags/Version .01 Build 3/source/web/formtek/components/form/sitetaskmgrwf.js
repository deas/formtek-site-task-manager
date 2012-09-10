function addReturnIcon()
{
	//  When the button is created, the span it will later reside in doesn't exist
	//    We add a class to the <span> after it has been added
    var button = Dom.getElementsByClassName("returnwfmarker");
    try
    {
	    if(button.length>0)
	    {
		    var child = Dom.getElementsByClassName("first-child", "span", button[0]);
		    Dom.addClass(child[0], "returnwf");
	    }
    }
    catch (err){}
}
