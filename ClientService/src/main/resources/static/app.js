function authToken() {
    return localStorage.getItem('reporting_jwt');
}
function authHeader() {
    const t = authToken();
    return t ? { Authorization: 'Bearer ' + t } : {};
}
function showLoggedIn(loggedIn) {
    $('#login_row').toggle(!loggedIn);
    $('#app_controls').toggle(loggedIn);
    if (!loggedIn) {
        $('#report_list_body').html('');
    }
}
function login() {
    const username = $('#login_username').val();
    const password = $('#login_password').val();
    $.ajax({
        url: '/auth/login',
        type: 'POST',
        data: JSON.stringify({ username: username, password: password }),
        contentType: 'application/json',
        dataType: 'json',
        success: function (data) {
            localStorage.setItem('reporting_jwt', data.token);
            $('#login_status').text('');
            showLoggedIn(true);
            loadAll();
        },
        error: function () {
            $('#login_status').addClass('text-danger').text('Invalid username or password');
        }
    });
}
function logout() {
    localStorage.removeItem('reporting_jwt');
    showLoggedIn(false);
}
function loadAll() {
    $("#report_list_body").html("");

    $.ajax({
        url: '/report',
        headers: authHeader(),
        dataType: 'json'
    }).done(
        function (data, textStatus, jqXHR) {  // success callback
            console.info(data);
            data.data.forEach((report, index)=>{
                $("#report_list_body").append(
                    $('<tr>').append(
                        $('<td>').append(index + 1)
                    ).append(
                        $('<td>').append(report.submitter)
                    ).append(
                        $('<td>').append(report.description)
                    ).append(
                        $('<td>').append(formatTime(report.createdTime))
                    ).append(
                        $('<td>').append(report.pdfReportStatus)
                    ).append(
                        $('<td>').append(report.excelReportStatus)
                    ).append(
                        "<td>" + actionLinks(report.pdfReportStatus, report.excelReportStatus, report.id, report.description) + "</td>"
                    )
                );
            });

        }
    ).fail(function (jqXHR) {
        if (jqXHR.status === 401) {
            logout();
        } else {
            alert('Error loading reports');
        }
    });
}
function formatTime(time) {
    if(!time){
        return "N/A";
    }
    const d = new Date(time);
    return singleDigit(d.getMonth() + 1) + '/'+singleDigit(d.getDate()) + ' ' + singleDigit(d.getHours()) + ':' + singleDigit(d.getMinutes());
}
function singleDigit(dig) {
    return ('0' + dig).slice(-2)
}
function downloadPDF(reqId){
    downloadFile('/report/content/'+reqId+ '/PDF');
}
function downloadExcel(reqId){
    downloadFile('/report/content/'+reqId+ '/EXCEL');
}
function downloadFile(urlToSend) {
    var req = new XMLHttpRequest();
    req.open("GET", urlToSend, true);
    var token = authToken();
    if (token) {
        req.setRequestHeader("Authorization", "Bearer " + token);
    }
    req.responseType = "blob";
    req.onload = function (event) {
        console.info(event);
        if(req.status === 200) {
            var blob = req.response;
            var fileName = req.getResponseHeader("fileName")
            var link = document.createElement('a');
            link.href = window.URL.createObjectURL(blob);
            link.download = fileName;
            link.click();
        } else{
            alert('Error in downloading')
        }
    };
    req.send();
}
function showDelete(reqId){
    if(confirm("Are you sure to delete report?")){
        $.ajax({
            url: '/report/' + reqId,
            type: 'DELETE',
            headers: authHeader(),
            success: function () { loadAll(); },
            error: function (jqXHR) {
                if (jqXHR.status === 401) { logout(); return; }
                alert('Error deleting report');
            }
        });
    }
}
function editReport(reqId, currentDescription){
    var description = prompt("New description:", decodeURIComponent(currentDescription));
    if (description === null || description.trim() === "") {
        return;
    }
    $.ajax({
        url: '/report/' + reqId,
        type: 'PUT',
        headers: authHeader(),
        data: JSON.stringify({ description: description }),
        contentType: 'application/json',
        dataType: 'json',
        success: function () { loadAll(); },
        error: function (jqXHR) {
            if (jqXHR.status === 401) { logout(); return; }
            alert('Error updating report');
        }
    });
}
function actionLinks(ps, es, id, description) {
    return (ps === 'COMPLETED'?"<a onclick='downloadPDF(\""+id+"\")' href='#'>Download PDF</a>":"")
        + (es === 'COMPLETED'?"<a onclick='downloadExcel(\""+id+"\")' style='margin-left: 1em' href='#'>Download Excel</a>":"")
        +"<a onclick='editReport(\""+id+"\", \""+encodeURIComponent(description)+"\")' style='margin-left: 1em' href='#'>Edit</a>"
        +"<a onclick='showDelete(\""+id+"\")' style='margin-left: 1em' href='#'>Delete</a>";
}
function validateInput(){
    try {
        return JSON.parse($('#inputData').val());
    }catch(err) {
        alert("This is not a valid Json.");
        return "";
    }
}

function submit(async) {
    let data = validateInput();
    if(!data) {
        return false;
    }
    $.ajax({
        url : async?"report/async":"report/sync",
        type: "POST",
        headers: authHeader(),
        data : JSON.stringify(data),
        contentType: "application/json",
        dataType: "json",
        success: function(data, textStatus, jqXHR)
        {
            console.info(data);
            $('#create_report_model').modal('toggle');
            loadAll();
        },
        error: function (jqXHR, textStatus, errorThrown) {
            if (jqXHR.status === 401) {
                logout();
                return;
            }
            alert(jqXHR.responseJSON ? jqXHR.responseJSON.message : 'Request failed');
            console.error(jqXHR);
            console.error(jqXHR.responseJSON.message);
        }
    });
}
$( document ).ready(function() {
    if (authToken()) {
        showLoggedIn(true);
        loadAll();
    } else {
        showLoggedIn(false);
    }
    $("#loginBtn").on("click", login);
    $("#logoutBtn").on("click", logout);
    $("#loadAllBtn").on("click",function () {
        loadAll();
    });
    $("#generateBtn").on("click",function () {
        $('#create_report_model').modal('toggle');
    });
    $("#create_report").on("click",function () {
        submit(false);
    });
    $("#create_report_async").on("click",function () {
        submit(true);
    });
});
