package org.mule.weave.v2.module.http.functions.utils

import org.mule.weave.v2.core.io.SeekableStream
import org.mule.weave.v2.model.EvaluationContext
import org.mule.weave.v2.model.structure.KeyValuePair
import org.mule.weave.v2.model.values.BinaryValue
import org.mule.weave.v2.model.values.KeyValue
import org.mule.weave.v2.model.values.NumberValue
import org.mule.weave.v2.model.values.ObjectValue
import org.mule.weave.v2.model.values.StringValue
import org.mule.weave.v2.model.values.Value
import org.mule.weave.v2.model.values.math.Number
import org.mule.weave.v2.module.http.functions.utils.HttpClientResponseConverter.BODY
import org.mule.weave.v2.module.http.functions.utils.HttpClientResponseConverter.CONTENT_TYPE
import org.mule.weave.v2.module.http.functions.utils.HttpClientResponseConverter.COOKIES
import org.mule.weave.v2.module.http.functions.utils.HttpClientResponseConverter.HEADERS
import org.mule.weave.v2.module.http.functions.utils.HttpClientResponseConverter.STATUS
import org.mule.weave.v2.module.http.functions.utils.HttpClientResponseConverter.STATUS_TEXT
import org.mule.weave.v2.module.http.service.HttpClientHeaders
import org.mule.weave.v2.module.http.service.HttpClientResponse
import org.mule.weave.v2.module.reader.SourceProvider

import java.net.HttpCookie
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

class HttpClientResponseConverter(response: HttpClientResponse) {

  def convert()(implicit ctx: EvaluationContext): ObjectValue = {
    val pairs = new ArrayBuffer[KeyValuePair]()

    // status
    pairs.+=(
      KeyValuePair(KeyValue(STATUS), NumberValue(Number(response.getStatus))))

    // statusText?
    response.getStatusText.ifPresent(st => {
      pairs.+=(
        KeyValuePair(KeyValue(STATUS_TEXT), StringValue(st)))
    })

    // headers
    pairs.+=(
      KeyValuePair(KeyValue(HEADERS), asHeadersValue(response.getHeaders)))

    // body?
    response.getBody.ifPresent(body => {
      val sourceProvider = SourceProvider(SeekableStream(body))
      pairs.+=(
        KeyValuePair(KeyValue(BODY), BinaryValue(sourceProvider.asInputStream)))
    })

    // cookies
    pairs.+=(
      KeyValuePair(
        KeyValue(COOKIES), asCookieValue(response.getCookies.asScala)))

    // contentType?
    response.getContentType.ifPresent(contentType => {
      pairs.+=(
        KeyValuePair(KeyValue(CONTENT_TYPE), StringValue(contentType)))
    })
    ObjectValue(pairs.toArray)
  }

  private def asHeadersValue(headers: HttpClientHeaders): Value[_] = {
    val names = headers.getHeaderNames.asScala.toArray

    val entries = names.flatMap(name => {
      headers.getHeaderValues(name).asScala.map(value => {
        KeyValuePair(KeyValue(name), StringValue(value))
      })
    })
    ObjectValue(entries)
  }

  private def asCookieValue(cookie: Seq[HttpCookie]): Value[_] = {
    val entries = cookie.map(cookie => {
      KeyValuePair(KeyValue(cookie.getName), StringValue(cookie.getValue))
    })
    ObjectValue(entries.toArray)
  }
}

object HttpClientResponseConverter {
  private val STATUS = "status"
  private val STATUS_TEXT = "statusText"
  private val HEADERS = "headers"
  private val BODY = "body"
  private val COOKIES = "cookies"
  private val CONTENT_TYPE = "contentType"

  def apply(response: HttpClientResponse): HttpClientResponseConverter =
    new HttpClientResponseConverter(response)
}
