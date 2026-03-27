import { useRef, useEffect } from 'react'
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'

interface EChartsWrapperProps {
  option: EChartsOption
  className?: string
  height?: string | number
  onChartReady?: (chart: echarts.ECharts) => void
}

export function EChartsWrapper({
  option,
  className,
  height = '400px',
  onChartReady,
}: EChartsWrapperProps) {
  const chartRef = useRef<HTMLDivElement>(null)
  const chartInstanceRef = useRef<echarts.ECharts | null>(null)

  // Initialize chart and update options
  useEffect(() => {
    if (!chartRef.current) return

    // Initialize chart instance once
    if (!chartInstanceRef.current) {
      chartInstanceRef.current = echarts.init(chartRef.current)
      onChartReady?.(chartInstanceRef.current)
    }

    const chart = chartInstanceRef.current

    // Update chart options (doesn't re-create the chart)
    chart.setOption(option, true)
  }, [option, onChartReady])

  // Handle window resize
  useEffect(() => {
    const chart = chartInstanceRef.current
    if (!chart) return

    const handleResize = () => {
      chart.resize()
    }
    window.addEventListener('resize', handleResize)

    return () => {
      window.removeEventListener('resize', handleResize)
    }
  }, [])

  // Handle parent/container resize (e.g., layout changes without window resize)
  useEffect(() => {
    const chart = chartInstanceRef.current
    if (!chart || !chartRef.current) return

    const resizeObserver = new ResizeObserver(() => {
      chart.resize()
    })

    resizeObserver.observe(chartRef.current)

    return () => {
      resizeObserver.disconnect()
    }
  }, [])

  // Cleanup: dispose chart on unmount only
  useEffect(() => {
    return () => {
      if (chartInstanceRef.current) {
        chartInstanceRef.current.dispose()
        chartInstanceRef.current = null
      }
    }
  }, [])

  return (
    <div
      ref={chartRef}
      className={className}
      style={{
        width: '100%',
        height: typeof height === 'number' ? `${height}px` : height,
      }}
    />
  )
}
