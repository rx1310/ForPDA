package forpdateam.ru.forpda.presentation.devdb.brands

import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.StateStrategyType
import forpdateam.ru.forpda.common.mvp.IBaseView
import forpdateam.ru.forpda.entity.remote.devdb.Brands

/**
 * Created by radiationx on 01.01.18.
 */

@StateStrategyType(AddToEndSingleStrategy::class)
interface BrandsView : IBaseView {
    fun showData(data: Brands)
    fun initCategories(categories: Array<String>, position: Int)
}
