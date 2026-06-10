const color = [
  "primary",
  "secondary",
  "success",
  "info",
  "warning",
  "error",
  "neutral"
] as const

const highlightColor = [
  "primary",
  "secondary",
  "success",
  "info",
  "warning",
  "error",
  "neutral"
] as const

const highlightVariant = [
  "straight",
  "circuit"
] as const

export default {
  "slots": {
    "root": "sticky top-(--ui-header-height) z-10 bg-default/75 lg:bg-[initial] backdrop-blur -mx-4 px-4 sm:px-6 sm:-mx-6 lg:ms-0 overflow-y-auto max-h-[calc(100vh-var(--ui-header-height))]",
    "container": "pt-4 sm:pt-6 pb-2.5 sm:pb-4.5 lg:py-8 border-b border-dashed border-default lg:border-0 flex flex-col",
    "top": "",
    "bottom": "hidden lg:flex lg:flex-col gap-6",
    "trigger": "group text-sm font-semibold flex-1 flex items-center gap-1.5 py-1.5 -mt-1.5 focus-visible:outline-primary",
    "title": "truncate",
    "trailing": "ms-auto inline-flex gap-1.5 items-center",
    "trailingIcon": "size-5 transform transition-transform duration-200 shrink-0 group-data-[state=open]:rotate-180 lg:hidden",
    "content": "relative data-[state=open]:animate-[collapsible-down_200ms_ease-out] data-[state=closed]:animate-[collapsible-up_200ms_ease-out] overflow-hidden focus:outline-none",
    "list": "min-w-0",
    "listWithChildren": "ms-3",
    "item": "min-w-0",
    "itemWithChildren": "",
    "link": "group relative text-sm flex items-center focus-visible:outline-primary py-1",
    "linkText": "truncate",
    "indicator": "",
    "indicatorLine": "",
    "indicatorActive": ""
  },
  "variants": {
    "color": {
      "primary": "",
      "secondary": "",
      "success": "",
      "info": "",
      "warning": "",
      "error": "",
      "neutral": ""
    },
    "highlightColor": {
      "primary": {
        "indicatorActive": "bg-primary"
      },
      "secondary": {
        "indicatorActive": "bg-secondary"
      },
      "success": {
        "indicatorActive": "bg-success"
      },
      "info": {
        "indicatorActive": "bg-info"
      },
      "warning": {
        "indicatorActive": "bg-warning"
      },
      "error": {
        "indicatorActive": "bg-error"
      },
      "neutral": {
        "indicatorActive": "bg-inverted"
      }
    },
    "active": {
      "false": {
        "link": [
          "text-muted hover:text-default",
          "transition-colors"
        ]
      }
    },
    "highlight": {
      "true": ""
    },
    "highlightVariant": {
      "straight": "",
      "circuit": ""
    },
    "body": {
      "true": {
        "bottom": "mt-6"
      }
    }
  },
  "compoundVariants": [
    {
      "color": "primary" as typeof color[number],
      "active": true,
      "class": {
        "link": "text-primary"
      }
    },
    {
      "color": "secondary" as typeof color[number],
      "active": true,
      "class": {
        "link": "text-secondary"
      }
    },
    {
      "color": "success" as typeof color[number],
      "active": true,
      "class": {
        "link": "text-success"
      }
    },
    {
      "color": "info" as typeof color[number],
      "active": true,
      "class": {
        "link": "text-info"
      }
    },
    {
      "color": "warning" as typeof color[number],
      "active": true,
      "class": {
        "link": "text-warning"
      }
    },
    {
      "color": "error" as typeof color[number],
      "active": true,
      "class": {
        "link": "text-error"
      }
    },
    {
      "color": "neutral" as typeof color[number],
      "active": true,
      "class": {
        "link": "text-highlighted"
      }
    },
    {
      "highlight": true,
      "highlightVariant": "straight" as typeof highlightVariant[number],
      "class": {
        "list": "ms-2.5 ps-4 border-s border-default",
        "item": "-ms-px",
        "indicator": "absolute ms-2.5 transition-[translate,height] duration-200 h-(--indicator-size) translate-y-(--indicator-position) w-px rounded-full",
        "indicatorLine": "hidden",
        "indicatorActive": "w-full h-full"
      }
    },
    {
      "highlight": true,
      "highlightVariant": "circuit" as typeof highlightVariant[number],
      "class": {
        "list": "ps-6.5",
        "item": "-ms-px",
        "itemWithChildren": "ps-px",
        "indicator": "absolute ms-2.5 start-0 top-0 rtl:-scale-x-100",
        "indicatorLine": "absolute inset-0 bg-(--ui-border)",
        "indicatorActive": "absolute w-full h-(--indicator-size) translate-y-(--indicator-position) transition-[translate,height] duration-200 ease-out"
      }
    }
  ],
  "defaultVariants": {
    "color": "primary" as typeof color[number],
    "highlightColor": "primary" as typeof highlightColor[number],
    "highlightVariant": "straight" as typeof highlightVariant[number]
  }
}