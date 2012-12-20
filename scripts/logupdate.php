<?
 
require_once('pass.php');
$tbl_name="LogUploads"; // Table name 
$tbl="User";
$column1="filename";
$column2="lastupload";
$column3="DeviceId";
// Connect to server and select database.
mysql_connect("$host", "$username", "$password")or die("cannot connect"); 
mysql_select_db("$db_name")or die("cannot select DB");

// Get values from form 
// TO do: Ensure that the device ID exists:TBD in design
//  - Device Table should have one more field - approved before logs can be uploaded

$uploadtime=$_POST['uploadtime'];

$devid=$_POST['devid'];
$numentries=$_POST['numentries'];
$filename=$_POST['filename'];
$swversion=$_POST['swversion'];
$nwoperator=$_POST['nwoperator'];
$starttime=$_POST['starttime'];
$endtime=$_POST['endtime'];
$longitude=$_POST['longitude'];
$latitude=$_POST['latitude'];
// Insert data into mysql 
$sqlLogEntry="INSERT INTO $tbl_name VALUES('','$uploadtime', '$devid', '$numentries','$filename','$swversion','$nwoperator','$starttime','$endtime','$longitude','$latitude')";
$sqlDevUpdate="UPDATE $tbl SET $column1='$filename' ,$column2='$uploadtime' WHERE $column3='$devid'; "; 
$result=mysql_query($sqlLogEntry);
$result1=mysql_query($sqlDevUpdate);

?>
