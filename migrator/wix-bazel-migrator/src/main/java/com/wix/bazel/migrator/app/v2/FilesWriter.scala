package com.wix.bazel.migrator.app.v2

/**
 * Actually writes stuff to disk
 */
trait FilesWriter {
  def appendToWorkspaceFile(content:String): Unit
  def appendToFile(fileToWrite: FileToWrite): Unit
}
case class FileToWrite(relativePathToFile: String, content: String)
