/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.util.mongo

import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.api.indexes.IndexType.Ascending

object IndexHelper {

  def createIndex(indexFieldsKey: Seq[(String, IndexType)], indexName: Option[String], isUnique: Boolean = false, isBackground: Boolean = true): Index =
    Index(key = indexFieldsKey, name = indexName, unique = isUnique, background = isBackground)

  def createSingleFieldAscendingIndex(indexFieldKey: String, indexName: Option[String], isUnique: Boolean = false, isBackground: Boolean = true): Index =
    Index(key = Seq(indexFieldKey -> Ascending), name = indexName, unique = isUnique, background = isBackground)

  def createAscendingIndex(indexName: Option[String], isUnique: Boolean, isBackground: Boolean, indexFieldsKey: String*): Index =
    Index(key = indexFieldsKey.map { _ -> Ascending }, name = indexName, unique = isUnique, background = isBackground)

}
