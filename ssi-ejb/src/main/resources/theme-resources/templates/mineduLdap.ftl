<#import "template.ftl" as layout>




<!DOCTYPE html>
<html>
    <head>
<#-- <title>ESMO sp-ms</title> -->
<!--        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"></meta>
        <meta charset="utf-8"></meta>-->
        <meta http-equiv="X-UA-Compatible" content="IE=edge"></meta>
        <meta name="viewport" content="width=device-width, initial-scale=1"></meta>
        <meta name="description" content=""></meta>
        <meta name="author" content=""></meta>
        <title>Create a new account</title>
        <meta http-equiv="cache-control" content="max-age=0" />
        <meta http-equiv="cache-control" content="no-cache" />
        <meta http-equiv="expires" content="0" />
        <meta http-equiv="expires" content="Tue, 01 Jan 1980 1:00:00 GMT" />
        <meta http-equiv="pragma" content="no-cache" />
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/materialize/1.0.0/css/materialize.min.css">
        <link href="https://fonts.googleapis.com/icon?family=Material+Icons" rel="stylesheet"></link>
        <link rel="stylesheet" href="${url.resourcesPath}/css/main.css"></link>
        <link rel="icon"
              type="image/x-icon"
              href="favicon.ico"></link>
        <link rel="icon"
              type="image/x-icon"
              href="favicon.ico"></link>

        </head>
    <body>

        <header>
            <div class="footerClass headerContainer">
                <div class=" instructions" style="width:90%">
                    <div class="row">
                        <div class="col s6 m6 l6 " id="header-logo">
                            <img id="companyLogo" class="responsive-img" src= "${url.resourcesPath}/img/logo2.png" style="margin-top: 1.8em;
                                 max-height: 55px;float:left;    padding-left: 40.5%;"/>
                            </div>
                        <div class="col s6 m6 l6 " id="header-logo-aegean">
                            <img id="uAegeanLogo" class="responsive-img" src="${url.resourcesPath}/img/uaegean_logo.png" style="margin-top: 0em;
                                 margin-bottom: 1.2em;
                                 max-height: 85px;float:right; margin-right: 5%;"/>
                            </div>
                        </div>    

                    </div>
                    <!--</div>-->


                </div>
            </header>



       <div class="container">
            <div class="row  mainContent">
                <div class="col s12 m12 l12">
                    <div class="container" style="width: 90%;">
                        <div class="row instructions">
                            <div   class="col s12 ">
                                <div class="row">
                                    <p  class="col s12">
                                        <b>Transfer your Famility Status Certificat</b>: Your Family Status Certificate will be request (via the Ministry of Education). Please provide the following info to proceed). 
                                    </p>
                                </div>
                                <form  id="ldapForm" action=${forward} method="post">
                                    <div class="row">
                                        <label class="col l1 s12" for="username">User Name:</label>
                                        <input class="col l7 s12"  name="username" type="text" id="username"/>
                                    </div>
                                    <div class="row">
                                        <label class="col l1 s12" for="password">Password:</label>
                                        <input class="col l7 s12"  name="password" type="password" id="password"/>
                                    </div>
                                    <input type="hidden" id="sessionId" name="sessionId" value=${sessionId} />
                                    <div class="row" id="buttons">
                                        <button type="button" onclick="onCancelClick()" class="col s12 m4 l4 waves-effect waves-light btn-large swell-btn cancel-btn" style="margin-right: 2em;">
                                            Cancel
                                        </button>
                                        <button  type="button" onclick="onNextClick()" class= "col s12 m4 l4 waves-effect waves-light btn-large swell-btn next-btn" value="Submit">Submit</button>
                                    </div>
                                    <div class="row" id="preloader" style="display:none">
                                        <div class="progress">
                                            <div class="indeterminate"></div>
                                        </div>
                                    </div>
                                </form>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

    

        <script type="text/javascript" src="https://code.jquery.com/jquery-2.1.1.min.js"></script>
        <script src="https://cdnjs.cloudflare.com/ajax/libs/materialize/1.0.0/js/materialize.min.js"></script>
        <script>
            function onNextClick() {
               let ldapForm = document.getElementById("ldapForm");
               let  buttons = document.getElementById("buttons");
               let  preloader = document.getElementById("preloader");
               let  notFound = document.getElementById("notFound");
               buttons.style.display = "none";
               preloader.style.display = "block";
  
               let username= document.getElementById("username").value;
               let password= document.getElementById("password").value;
               let sessionId = document.getElementById("sessionId").value;

               let xhttp = new XMLHttpRequest();
               xhttp.onreadystatechange = function() {
                    if (xhttp.readyState == XMLHttpRequest.DONE) {
                       if (xhttp.status == 200) {
                           console.log(xhttp.responseText);
                               window.location.href ="${proceed}?sessionId="+sessionId;
                       }
                       else if (xhttp.status == 400) {
                          console.log('There was an error 400');
                          preloader.style.display = "none";
                          notFound.style.display = "block";
                       }
                       else {
                           console.log('something else other than 200 was returned');
                            preloader.style.display = "none";
                       }
                    }
                };
                xhttp.open("POST", "${forward}", true);
                xhttp.setRequestHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
                let postUrlData = encodeURI("username="+username+"&password="+password+"&sessionId="+sessionId);
                xhttp.send(postUrlData);
                   
            }
    </script>

    </body>
</html>




