<?php
require_once('pass.php');

$tbl_name="User"; // Table name
$column1="Id";
$column2="Name";
$column3="EmailAddress";
$column4="PhoneNumber";
$column5="location";
$column6="DeviceId";

$required = array('name', 'email', 'location', 'contact', 'DeviceId');
// Loop over field names, make sure each one exists and is not empty
$error = false;
foreach($required as $field) {
  if (empty($_POST[$field])) {
    $error = true;
  }
}
if ($error) {
  die("All fields are required.");
} else {
  echo "Proceed...";
}

// Connect to server and select database.
mysql_connect("$host", "$username", "$password")or die("cannot connect");
mysql_select_db("$db_name")or die("cannot select DB");

// Get values from form
// mysql_real_escape_string

$name=mysql_real_escape_string($_POST['name']);

$email=mysql_real_escape_string($_POST['email']);
$location=mysql_real_escape_string($_POST['location']);
$contact=mysql_real_escape_string($_POST['contact']);
$deviceid=mysql_real_escape_string($_POST['DeviceId']);
// Insert data into mysql
$sql="INSERT INTO $tbl_name (Id,Name,EmailAddress,PhoneNumber,location,DeviceId) VALUES ('','$name', '$email', '$contact','$location','$deviceid')";
$result=mysql_query($sql);
print "DB Insert completed with  $result";

?>

