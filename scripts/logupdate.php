<?php
require_once('pass.php');
$tbl_name="LogUploads"; // Table name 
$usrtbl="User";
$column1="filename";
$column2="lastupload";
$column3="DeviceId";


$required = array('uploadtime', 'devid', 'filename', 'swversion', 'nwoperator');
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

// mysql_real_escape_string

// Get values from form 
// TO do: Ensure that the device ID exists:TBD in design
//  - Device Table should have one more field - approved before logs can be uploaded
$uploadtime=mysql_real_escape_string($_POST['uploadtime']);
$devid=mysql_real_escape_string($_POST['devid']);
$numentries=10; //dummy
$filename=mysql_real_escape_string($_POST['filename']);
$swversion=mysql_real_escape_string($_POST['swversion']);
$nwoperator=mysql_real_escape_string($_POST['nwoperator']);
$starttime='dummy';
$endtime='dummy';
$longitude='dummy';
$latitude='dummy';

echo "Got Message Loc 2";
// Insert data into mysql 
$sqlLogEntry="INSERT INTO $tbl_name VALUES('','$uploadtime', '$devid', '$numentries','$filename','$swversion','$nwoperator','$starttime','$endtime','$longitude','$latitude')";
$sqlDevUpdate="UPDATE $usrtbl SET $column1='$filename' ,$column2='$uploadtime' WHERE $column3='$devid'; "; 
echo "Trying command $sqlLogEntry";
echo "and command $sqlDevUpdate";

$result=mysql_query($sqlLogEntry);
$result1=mysql_query($sqlDevUpdate);

?>

