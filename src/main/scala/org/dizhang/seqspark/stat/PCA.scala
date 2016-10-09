package org.dizhang.seqspark.stat

import breeze.linalg.{DenseMatrix => BDM}
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.mllib.feature.{PCA => SPCA}
import org.apache.spark.rdd.RDD
import org.dizhang.seqspark.ds.{DenseCounter, Genotype, SparseCounter}
import org.dizhang.seqspark.worker.Data
import org.dizhang.seqspark.util.General._
import org.slf4j.LoggerFactory

/**
  * perform PCA for
  */
@SerialVersionUID(103L)
class PCA[A: Genotype](vcf: Data[A]) extends Serializable {

  val logger = LoggerFactory.getLogger(getClass)

  def geno = implicitly[Genotype[A]]

  def prepare: RDD[Vector] = {

    vcf.map{v =>
      val af = v.toCounter(geno.toAAF, (0.0, 2.0)).reduce.ratio
      val cnt = v.toCounter(geno.toBRV(_, af), 0.0)
      cnt match {
        case DenseCounter(elems, _) => Vectors.dense(elems.toArray)
        case SparseCounter(elems, _, n) => Vectors.sparse(n, elems.toSeq.sortBy(_._1))
      }
    }

    /**
      * val input = vcf
    val byColAndRow = input.zipWithIndex().flatMap{
      case (v, ri) =>
        val maf = v.toCounter(geno.toAAF, (0.0, 2.0)).reduce.ratio
        v.toIndexedSeq.zipWithIndex.map{
        case (g, ci) => ci -> (ri, geno.toBRV(g, maf))
      }
    }
    val byCol = byColAndRow.groupByKey().sortByKey().values
    val res = byCol.map {
      row => Vectors.dense(row.toArray.sortBy(_._1).map(_._2))
    }

    res.cache()
    logger.info(s"PCA transposed data dimension: ${res.count()} x ${res.first().size}")
    res
      */
  }
  def pc(n: Int): BDM[Double] = {
    val model = new SPCA(n)
    val data = this.prepare
    val res = model.fit(data).pc.values
    new BDM(res.length/n, n, res)
  }
}