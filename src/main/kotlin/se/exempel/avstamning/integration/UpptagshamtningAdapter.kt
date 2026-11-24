package se.exempel.avstamning.integration

import se.exempel.avstamning.app.port.Upptagshamtning
import se.exempel.avstamning.domain.Kallsystem
import se.exempel.avstamning.domain.Tjanstuppgifter
import se.exempel.avstamning.domain.Upptagsuppgifter
import se.exempel.avstamning.integration.kallsystem.SystemAClient
import se.exempel.avstamning.integration.kallsystem.SystemBClient
import se.exempel.avstamning.integration.kallsystem.SystemCClient
import se.exempel.avstamning.integration.tjanst.UpptagstjanstClient
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService

class UpptagshamtningAdapter(
    private val systemA: SystemAClient,
    private val systemB: SystemBClient,
    private val systemC: SystemCClient,
    private val tjanstX: UpptagstjanstClient,
    private val tjanstY: UpptagstjanstClient,
    private val tjanstZ: UpptagstjanstClient,
    private val executor: ExecutorService,
) : Upptagshamtning {

    override fun hamtaFranKallsystem(kallsystem: Kallsystem, kallreferens: String): Upptagsuppgifter =
        when (kallsystem) {
            Kallsystem.SYSTEM_A -> systemA.hamta(kallreferens)
            Kallsystem.SYSTEM_B -> systemB.hamta(kallreferens)
            Kallsystem.SYSTEM_C -> systemC.hamta(kallreferens)
        }

    /** Alla tre anrop startas innan vi väntar på svaren. */
    override fun hamtaFranTjanster(upptagsId: String): Tjanstuppgifter {
        val x = executor.submit(Callable { tjanstX.hamta(upptagsId) })
        val y = executor.submit(Callable { tjanstY.hamta(upptagsId) })
        val z = executor.submit(Callable { tjanstZ.hamta(upptagsId) })
        return Tjanstuppgifter(tjanstX = x.get(), tjanstY = y.get(), tjanstZ = z.get())
    }
}
