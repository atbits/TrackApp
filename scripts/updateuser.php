<?
require_once('pass.php');
 
$tbl_name="User"; // Table name 
$column1="Id";
$column2="Name";
$column3="EmailAddress";
$column4="PhoneNumber";
$column5="location";
$column6="DeviceId";
// Connect to server and select database.
mysql_connect("$host", "$username", "$password")or die("cannot connect"); 
mysql_select_db("$db_name")or die("cannot select DB");

// Get values from form 


$name=$_POST['name'];

$email=$_POST['email'];
$location=$_POST['location'];
$contact=$_POST['contact'];
$deviceid=$_POST['DeviceId'];
// Insert data into mysql 
//$sql="INSERT INTO $tbl_name (Id,Name,EmailAddress,PhoneNumber,location,DeviceId) VALUES ('','$name', '$email', '$contact','$location','$deviceid')";

$sql="UPDATE $tbl_name SET $column2='$name',$column4='$contact',$column6='$deviceid' WHERE $column3='$email'; ";

$result=mysql_query($sql);
print "Usr details updated as requested";

?>
