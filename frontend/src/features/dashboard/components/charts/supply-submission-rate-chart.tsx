import { useCallback, useEffect, useMemo, useRef, useState, type PointerEvent } from 'react'
import { Box, useBreakpointValue, useTheme } from '@chakra-ui/react'
import * as echarts from 'echarts'
import { EChartsWrapper } from './echarts-wrapper'
import { getBodyText7Style } from './chart-text-style'
import type { EntityPerformance } from '../../types'

interface SupplySubmissionRateChartProps {
  data: EntityPerformance[]
  className?: string
  height?: string | number
  maxItems?: number
  entityLabel?: string
}

export function SupplySubmissionRateChart({
  data,
  className,
  height = '500px',
  maxItems = 5,
  entityLabel = 'States/UTs',
}: SupplySubmissionRateChartProps) {
  const theme = useTheme()
  const bodyText7 = getBodyText7Style(theme)
  const barWidth = useBreakpointValue({ base: 28, sm: 28, md: 42, lg: 66 }) ?? 66
  const barRadius = useBreakpointValue({ base: 8, sm: 10, md: 12 }) ?? 12
  const normalizedMaxItems =
    typeof maxItems === 'number' && Number.isFinite(maxItems) ? Math.max(1, maxItems) : 1
  const chartScrollRef = useRef<HTMLDivElement>(null)
  const scrollbarTrackRef = useRef<HTMLDivElement>(null)
  const scrollbarThumbRef = useRef<HTMLDivElement>(null)
  const isDraggingThumb = useRef(false)
  const dragStartX = useRef(0)
  const dragStartLeft = useRef(0)
  const thumbLeftRef = useRef(0)
  const [containerWidth, setContainerWidth] = useState(0)

  const defaultItemWidth = 90
  const minItemWidth = 70
  const effectiveItemWidth =
    containerWidth > 0
      ? Math.max(minItemWidth, Math.floor(containerWidth / Math.max(data.length, 1)))
      : defaultItemWidth
  const itemWidth = Math.min(defaultItemWidth, effectiveItemWidth)
  const axisWidth = '56px'
  const axisLabelOffset = '-25px'
  const dynamicBarWidth = Math.min(barWidth, Math.max(12, Math.floor(itemWidth * 0.6)))
  const longestEntityLabel = useMemo(() => {
    return data.reduce((longest, item) => {
      return item.name.length > longest.length ? item.name : longest
    }, '')
  }, [data])

  const option = useMemo<echarts.EChartsOption>(() => {
    const entities = data.map((d) => d.name)
    const rates = data.map((d) => d.regularity)

    return {
      tooltip: {
        show: false,
      },
      grid: {
        left: '0%',
        right: '4%',
        top: '10%',
        bottom: '5%',
        containLabel: true,
      },
      xAxis: {
        type: 'category',
        data: entities,
        axisLine: {
          lineStyle: {
            color: '#E4E4E7',
          },
        },
        axisTick: {
          show: false,
        },
        axisLabel: {
          rotate: 45,
          interval: 0,
          margin: 8,
          fontSize: bodyText7.fontSize,
          lineHeight: bodyText7.lineHeight,
          fontWeight: 400,
          color: bodyText7.color,
        },
      },
      yAxis: {
        type: 'value',
        axisLabel: {
          show: false,
        },
        max: 100,
        interval: 25,
        splitLine: {
          lineStyle: {
            color: '#E4E4E7',
          },
        },
      },
      series: [
        {
          name: 'Submission Rate',
          type: 'bar',
          data: rates,
          barWidth: dynamicBarWidth,
          barCategoryGap: '45%',
          itemStyle: {
            color: '#3291D1',
            borderRadius: [barRadius, barRadius, barRadius, barRadius],
          },
          emphasis: {
            disabled: true,
          },
        },
      ],
    }
  }, [data, dynamicBarWidth, barRadius, bodyText7])

  const axisOption = useMemo<echarts.EChartsOption>(() => {
    const placeholderLabel = longestEntityLabel || 'W'
    return {
      tooltip: {
        show: false,
      },
      grid: {
        left: '20%',
        right: 0,
        top: '10%',
        bottom: '5%',
        containLabel: true,
      },
      xAxis: {
        type: 'category',
        data: [placeholderLabel],
        axisTick: {
          show: false,
        },
        axisLine: {
          show: false,
        },
        axisLabel: {
          show: true,
          rotate: 45,
          margin: 8,
          fontSize: bodyText7.fontSize,
          lineHeight: bodyText7.lineHeight,
          fontWeight: 400,
          formatter: (value: string) => value,
          color: 'transparent',
        },
      },
      yAxis: {
        type: 'value',
        position: 'right',
        axisLabel: {
          align: 'right',
          margin: 5,
          fontSize: bodyText7.fontSize,
          lineHeight: bodyText7.lineHeight,
          fontWeight: 400,
          color: bodyText7.color,
        },
        min: 0,
        max: 100,
        interval: 25,
        splitLine: {
          show: false,
        },
      },
      series: [
        {
          type: 'bar',
          data: [0],
          barWidth: 0,
          itemStyle: {
            opacity: 0,
          },
        },
      ],
      animation: false,
    }
  }, [bodyText7, longestEntityLabel])

  const baseChartWidth = data.length * itemWidth
  const chartPixelWidth =
    containerWidth > 0 ? Math.max(baseChartWidth, containerWidth) : baseChartWidth
  const shouldScroll =
    data.length > normalizedMaxItems && containerWidth > 0 && baseChartWidth > containerWidth
  const chartWidth = shouldScroll ? `${chartPixelWidth}px` : '100%'

  const getTrackWidth = () => {
    return scrollbarTrackRef.current?.getBoundingClientRect().width ?? 0
  }

  const updateThumbFromScroll = useCallback(() => {
    const node = chartScrollRef.current
    const thumb = scrollbarThumbRef.current
    if (!node || !thumb) return
    const trackWidth = getTrackWidth()
    if (trackWidth === 0) return
    const thumbWidth = Math.min(163, trackWidth)
    const maxThumbTravel = Math.max(0, trackWidth - thumbWidth)
    const maxScroll = node.scrollWidth - node.clientWidth
    const nextLeft =
      maxScroll <= 0 || maxThumbTravel === 0 ? 0 : (node.scrollLeft / maxScroll) * maxThumbTravel
    thumb.style.width = `${thumbWidth}px`
    thumb.style.left = `${nextLeft}px`
    thumbLeftRef.current = nextLeft
  }, [])

  useEffect(() => {
    const node = chartScrollRef.current
    if (!node) return

    const resizeObserver = new ResizeObserver((entries) => {
      const entry = entries[0]
      if (entry) setContainerWidth(entry.contentRect.width)
      updateThumbFromScroll()
    })

    resizeObserver.observe(node)
    return () => {
      resizeObserver.disconnect()
    }
  }, [updateThumbFromScroll])

  useEffect(() => {
    const node = scrollbarTrackRef.current
    if (!node) return

    const resizeObserver = new ResizeObserver(() => {
      updateThumbFromScroll()
    })

    resizeObserver.observe(node)
    return () => {
      resizeObserver.disconnect()
    }
  }, [updateThumbFromScroll])

  const handleThumbPointerDown = (event: PointerEvent<HTMLDivElement>) => {
    if (!shouldScroll) return
    isDraggingThumb.current = true
    dragStartX.current = event.clientX
    dragStartLeft.current = thumbLeftRef.current
    event.currentTarget.setPointerCapture(event.pointerId)
  }

  const handleThumbPointerMove = (event: PointerEvent<HTMLDivElement>) => {
    if (!isDraggingThumb.current) return
    const node = chartScrollRef.current
    if (!node) return
    const trackWidth = getTrackWidth()
    if (trackWidth === 0) return
    const thumbWidth = Math.min(163, trackWidth)
    const maxThumbTravel = Math.max(0, trackWidth - thumbWidth)
    if (maxThumbTravel === 0) return
    const delta = event.clientX - dragStartX.current
    const nextLeft = Math.min(Math.max(dragStartLeft.current + delta, 0), maxThumbTravel)
    const thumb = scrollbarThumbRef.current
    if (thumb) {
      thumb.style.left = `${nextLeft}px`
    }
    thumbLeftRef.current = nextLeft
    const maxScroll = node.scrollWidth - node.clientWidth
    node.scrollLeft = (nextLeft / maxThumbTravel) * maxScroll
  }

  const handleThumbPointerUp = (event: PointerEvent<HTMLDivElement>) => {
    if (!isDraggingThumb.current) return
    isDraggingThumb.current = false
    event.currentTarget.releasePointerCapture(event.pointerId)
  }

  const handleThumbPointerCancel = (event: PointerEvent<HTMLDivElement>) => {
    if (!isDraggingThumb.current) return
    isDraggingThumb.current = false
    event.currentTarget.releasePointerCapture(event.pointerId)
  }

  useEffect(() => {
    updateThumbFromScroll()
  }, [data.length, containerWidth, updateThumbFromScroll])

  return (
    <div
      className={className}
      style={{
        width: '100%',
        minWidth: 0,
        height,
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      <Box flex={1} minH={0} minW={0} overflow="visible" display="flex">
        <Box width={axisWidth} flexShrink={0} position="relative">
          <EChartsWrapper option={axisOption} height="100%" />
          <Box
            position="absolute"
            left={axisLabelOffset}
            top="50%"
            transform="translateY(-50%) rotate(-90deg)"
            transformOrigin="center"
            textStyle="bodyText7"
            fontWeight="400"
            color={bodyText7.color}
            whiteSpace="nowrap"
          >
            Percentage
          </Box>
        </Box>
        <Box
          ref={chartScrollRef}
          overflowX={shouldScroll ? 'auto' : 'hidden'}
          overflowY="hidden"
          height="100%"
          flex="1"
          minW={0}
          onScroll={updateThumbFromScroll}
          sx={{
            scrollbarWidth: 'none',
            '&::-webkit-scrollbar': { height: '0px' },
          }}
        >
          <div
            style={{
              width: chartWidth,
              height: '100%',
            }}
          >
            <EChartsWrapper option={option} height="100%" />
          </div>
        </Box>
      </Box>
      <Box
        textAlign="center"
        textStyle="bodyText7"
        fontWeight="400"
        color={bodyText7.color}
        mt="4px"
      >
        {entityLabel}
      </Box>
      <Box mt="6px">
        <Box
          ref={scrollbarTrackRef}
          height="4px"
          bg="neutral.200"
          borderRadius="999px"
          position="relative"
        >
          <Box
            role="presentation"
            position="absolute"
            top={0}
            height="4px"
            width="163px"
            maxW="100%"
            bg="neutral.300"
            borderRadius="999px"
            cursor={shouldScroll ? 'grab' : 'default'}
            ref={scrollbarThumbRef}
            onPointerDown={handleThumbPointerDown}
            onPointerMove={handleThumbPointerMove}
            onPointerUp={handleThumbPointerUp}
            onPointerLeave={handleThumbPointerUp}
            onPointerCancel={handleThumbPointerCancel}
          />
        </Box>
      </Box>
    </div>
  )
}
