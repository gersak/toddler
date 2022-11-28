import {FaTimes, FaCheck, FaSearch, FaChevronDown, FaQuoteLeft, FaMinus,
        FaAngleRight, FaAngleLeft, FaSquare, FaCheckSquare, FaPlus, FaBarcode,
        FaEdit, FaCaretUp, FaCaretDown, FaCaretRight, FaAngleDoubleLeft,
        FaAngleDoubleRight, FaExclamationTriangle, FaList, FaCalendarWeek} from 'react-icons/fa';


let icons = {
  close: FaTimes,
  clear: FaTimes,
  checkboxDefault: FaMinus,
  checkbox: FaCheck,
  search: FaSearch,
  dropdownDecorator: FaChevronDown,
  info: FaQuoteLeft,
  next: FaAngleRight,
  previous: FaAngleLeft,
  checklistSelected: FaCheckSquare,
  checklistEmpty: FaSquare,
  warning: FaExclamationTriangle,
  // Table
  uuid: FaBarcode,
  add: FaPlus,
  edit: FaEdit,
  sortDesc: FaCaretUp,
  sortAsc: FaCaretDown,
  expand: FaCaretRight,
  selectedRow: FaAngleRight,
  // Pagination
  paginationFarNext: FaAngleDoubleRight,
  paginationFarPrevious: FaAngleDoubleLeft,
  paginationNext: FaAngleRight,
  paginationPrevious: FaAngleLeft,
  // Header filters
  timeFilter: FaCalendarWeek,
  enumFilter: FaList
}

export default icons;
