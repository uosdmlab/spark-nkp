package com.github.uosdmlab.nkp

import org.apache.spark.ml.UnaryTransformer
import org.apache.spark.ml.param.{ParamMap, Params, StringArrayParam}
import org.apache.spark.ml.util.{DefaultParamsReadable, DefaultParamsWritable, Identifiable}
import org.apache.spark.sql.types.{ArrayType, DataType, StringType}

private[nkp] trait TokenizerParams extends Params {

  final val filter: StringArrayParam = new StringArrayParam(this, "filter", "POS(Part Of Speech) filter")

  final def getFilter: Array[String] = $(filter)
}

/**
  * Created by jun on 2016. 10. 23..
  */
class Tokenizer(override val uid: String) extends UnaryTransformer[String, Seq[String], Tokenizer]
  with TokenizerParams with DefaultParamsWritable {

  import org.bitbucket.eunjeon.seunjeon.{Analyzer => EunjeonAnalyzer, LNode}

  def this() = this(Identifiable.randomUID("nkp_t"))

  def setFilter(value: Array[String]): this.type = set(filter, value)

  def setFilter(value: Seq[String]): this.type = setFilter(value.toArray)

  def setFilter(value: String, values: String*): this.type = setFilter(value +: values)

  setDefault(filter -> Array.empty[String])

  override protected def createTransformFunc: String => Seq[String] = { text: String =>
    val parsed: Seq[LNode] = EunjeonAnalyzer.parse(text) // Parse text using seunjeon

    val words: Seq[String] =
      if ($(filter).length == 0) parsed.map(_.morpheme.surface)
      else parsed.map { lNode: LNode =>
        val mor = lNode.morpheme // morpheme
        val poses = mor.poses.map(_.toString) intersect $(filter)  // filter with POS

        if (poses.nonEmpty) Some(mor.surface) else None
      }.filter(_.nonEmpty)
        .map(_.get)

    words
  }

  override protected def validateInputType(inputType: DataType): Unit = {
    require(inputType == StringType, s"Input type should be String")
  }

  override protected def outputDataType: DataType = new ArrayType(StringType, true)

  override def copy(extra: ParamMap): Tokenizer = defaultCopy(extra)
}

object Tokenizer extends DefaultParamsReadable[Tokenizer] {

  override def load(path: String): Tokenizer = super.load(path)
}