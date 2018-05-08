package io.sportadvisor.util.i18n

import org.slf4s.Logging

import scala.collection.mutable.{ArrayBuffer, Map => MMap}
import scaposer.{Parser, I18n => scaI18n}

import scala.io.Source

/**
  * @author sss3 (Vladimir Alekseev)
  */
private[i18n] object PoLoader extends Logging {

  private case class CacheKey(language: String, bundle: String)

  private val cache = MMap.empty[CacheKey, scaI18n]

  def get(language: String, bundle: String): scaI18n = {
    val key = CacheKey(language, bundle)
    if (cache.isDefinedAt(key)) return cache(key)

    synchronized {
      val urlEnum = Thread.currentThread.getContextClassLoader
        .getResources("i18n/" + bundle + "/" + language + ".po")
      val buffer = ArrayBuffer.empty[scaI18n]
      while (urlEnum.hasMoreElements) {
        val url = urlEnum.nextElement()
        val is = url.openStream()
        val string = Source.fromInputStream(is).mkString
        Parser.parse(string) match {
          case Left(parseFailure) =>
            log.warn(s"Could not load $url: $parseFailure")

          case Right(translations) =>
            val i18n = scaI18n(translations)
            buffer.append(i18n)
        }
      }

      val ret = buffer.foldLeft(new scaI18n(Map.empty)) { (acc, e) =>
        acc ++ e
      }
      cache(key) = ret
      ret
    }

  }
}
