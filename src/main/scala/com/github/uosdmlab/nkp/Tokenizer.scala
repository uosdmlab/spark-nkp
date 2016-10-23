package com.github.uosdmlab.nkp

import org.apache.spark.ml.UnaryTransformer
import org.apache.spark.ml.param.{Param, ParamMap, Params, StringArrayParam}
import org.apache.spark.ml.util.{DefaultParamsReadable, DefaultParamsWritable, Identifiable}
import org.apache.spark.sql.types.{ArrayType, DataType, StringType}

private[nkp] trait TokenizerParams extends Params {

  final val posFilter: StringArrayParam = new StringArrayParam(this, "posFilter", "POS(Part Of Speech) filter")

  final def getPosFilter: Array[String] = $(posFilter)
}

/**
  * Created by jun on 2016. 10. 23..
  */
class Tokenizer(override val uid: String) extends UnaryTransformer[String, Seq[String], Tokenizer]
  with TokenizerParams with DefaultParamsWritable {

  import org.bitbucket.eunjeon.seunjeon.{Analyzer => EunjeonAnalyzer, LNode}

  def this() = this(Identifiable.randomUID("nkp"))

  def setPosFilter(value: Array[String]): this.type = set(posFilter, value)

  def setPosFilter(value: Seq[String]): this.type = setPosFilter(value.toArray)

  def setPosFilter(value: String, values: String*): this.type = setPosFilter(value +: values)

  setDefault(posFilter -> Array.empty[String])

  override protected def createTransformFunc: String => Seq[String] = { text: String =>
    val parsed: Seq[LNode] = EunjeonAnalyzer.parse(text) // Parse text using seunjeon

    val words: Seq[String] =
      if ($(posFilter).size == 0) parsed.map(_.morpheme.surface)
      else parsed.map { lNode: LNode =>
        val mor = lNode.morpheme // morpheme
        val poses = mor.poses.map(_.toString) intersect $(posFilter)  // filter with POS

        if (poses.size > 0) Some(mor.surface) else None
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