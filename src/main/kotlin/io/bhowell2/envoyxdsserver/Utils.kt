//package io.bhowell2.envoyxdsserver
//
//import io.vertx.core.json.JsonObject
//import java.io.File
//import java.io.InputStream
//import java.net.URL
//import java.nio.file.Files
//import java.nio.file.Path
//import java.nio.file.Paths
//import java.util.*
//import java.util.stream.Collectors
//
///**
// * @author Blake Howell
// */
///**
// * @param resourceName
// * @return
// */
//fun getResourceAsStream(resourceName: String): InputStream {
//  return Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
//}
//
///**
// * Creates JsonObject from a resource. The resource must be a json file
// *
// * @param resourceName name of resource in test/resources to load and convert to a JsonObject
// * @return
// * @throws Exception
// */
//@Throws(Exception::class)
//fun getResourceAsJsonObject(resourceName: String): JsonObject {
//  val inputStream = getResourceAsStream(resourceName);
//  val s = Scanner(inputStream).useDelimiter("\\A");
//  if (s.hasNext()) {
//    return JsonObject(s.next());
//  } else
//    throw Exception("Resource is not of json type.");
//}
//
///**
// * Loads the specified resource as a file.
// *
// * @param resourceName name of file in test/resources to load
// * @return the file
// * @throws Exception
// */
//fun getResourceAsFile(resourceName: String): File {
//  return File(Thread.currentThread().getContextClassLoader().getResource(resourceName).toURI());
//}
//
//fun getResourceAsString(resourceName: String): String {
//  return Files.readAllLines(getResourcePath(resourceName)).stream().collect(Collectors.joining("\n"));
//}
//
//fun getResourcePath(resourceName: String): Path {
//  return Paths.get(Thread.currentThread().getContextClassLoader().getResource(resourceName).toURI());
//}
//
///**
// * Sometimes files are packaged in jars or on the classpath and other times files should be loaded from an absolute
// * path on the file system. This provides a flexible way to attempt to load the file. This uses the system classloader.
// * If another classloader is desired, use the overloaded method.
// * @param fileName
// * @return
// */
//@Throws(Exception::class)
//fun loadFileFromResourceOrFileSystem(fileName: String): File {
//  return loadFileFromResourceOrFileSystem(ClassLoader.getSystemClassLoader(), fileName)
//}
//
///**
// * Sometimes files are packaged in jars or on the classpath and other times files should be loaded from an absolute
// * path on the file system. This provides a flexible way to attempt to load the file.
// * @param classLoader
// * @param fileName
// * @return
// * @throws Exception
// */
//@Throws(Exception::class)
//fun loadFileFromResourceOrFileSystem(classLoader: ClassLoader, fileName: String): File {
//  var fileUrl: URL? = null
//  fileUrl = classLoader.getResource(fileName)
//  var file: File? = null
//  if (fileUrl != null) {
//    file = File(fileUrl.toURI())
//  }
//  // if privKeyFile is still null, that means was not able to load the file from the classpath, try system path
//  if (file == null) {
//    file = File(fileName)
//  }
//  return file
//}
