import React, {Component} from 'react';
import { withRouter } from 'react-router-dom';
import './Common.css';
import './AgentRecords.css';
import MessagesLoader from "./MessagesLoader";
import cx from 'classnames';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faCaretSquareUp, faCaretSquareDown } from '@fortawesome/free-solid-svg-icons'

class AgentRecords extends Component {
  constructor(props) {
    super(props);
    this.state = {
      globalError: '',
      siteName: '',
      messages: MessagesLoader.Empty,
      records: [],
      pageControl: {
        currentPage: 0,
        pageSize: 10,
        pageCount: 0,
        nextPageExists: false,
        prevPageExists: false
      }
    };
  }

  msg = (key) => {
    return this.state.messages(key);
  }

  renderAgentRecords = async(page, orderBy) => {
    try {
      const url = "/agentRecords?siteId=" + this.props.match.params.siteId + "&page=" + page + "&orderBySpec=" + encodeURI(orderBy);
      const resp = await fetch(url);

      if (resp.status === 200) {
        const json = await resp.json();
        this.setState({
          pageControl: json.pageControl,
          pagination: json.pagination,
          siteName: json.site.siteName,
          records: json.table
        });
      } else if (resp.status === 404) {
        const json = await resp.json();
        this.setState({
          globalError: json.errorMessage
        });
      } else {
        console.log("error: " + resp.status);
        this.setState({
          globalError: this.msg('error.unknown')
        });
      }
    } catch (e) {
      console.log("Error: " + JSON.stringify(e));
      this.setState({
        globalError: this.msg('error.unknown')
      });
    }
  }

  componentDidMount = async() => {
    try {
      this.setState({
        messages: await new MessagesLoader().load([
          {key: 'recordEmpty'},
          {key: 'registerMyRecord'},
          {key: 'rank'},
          {key: 'scoreList'},
          {key: 'abbrevAgentName'},
          {key: 'abbrevFaction'},
          {key: 'abbrevStartLevel'},
          {key: 'abbrevEndLevel'},
          {key: 'abbrevEarnedLevel'},
          {key: 'abbrevStartAp'},
          {key: 'abbrevEndAp'},
          {key: 'abbrevEarnedAp'},
          {key: 'abbrevStartWalked'},
          {key: 'abbrevEndWalked'},
          {key: 'abbrevEarnedWalked'},
          {key: 'abbrevUpdatedTime'}
        ])
      });
    } catch (e) {
      console.log("Error: " + JSON.stringify(e));
      this.setState({
        globalError: this.msg('error.unknown')
      });
    }

    this.renderAgentRecords(0, "lifetime_ap_earned DESC");
  }

  registerMyRecord = () => {
    this.props.history.push("/attend/" + this.props.match.params.siteId);
  }

  nextPage = () => {
    this.renderAgentRecords(
      this.state.pageControl.currentPage + 1, this.state.pageControl.orderByCol + " " + this.state.pageControl.orderBySort
    );
  }

  prevPage = () => {
    this.renderAgentRecords(
      this.state.pageControl.currentPage - 1, this.state.pageControl.orderByCol + " " + this.state.pageControl.orderBySort
    );
  }

  gotoPage = (p) => {
    this.renderAgentRecords(
      p, this.state.pageControl.orderByCol + " " + this.state.pageControl.orderBySort
    );
  }

  renderOrderByNum = (colName) => {
    if (colName === this.state.pageControl.orderByCol) {
      if (this.state.pageControl.orderBySort === "ASC") {
        return <span className="orderBy asc"><FontAwesomeIcon icon={faCaretSquareUp}/></span>;
      } else {
        return <span className="orderBy desc"><FontAwesomeIcon icon={faCaretSquareDown}/></span>;
      }
    } else {
      return <span className="orderBy"></span>;
    }
  }

  renderOrderByChar = (colName) => {
    if (colName === this.state.pageControl.orderByCol) {
      if (this.state.pageControl.orderBySort === "ASC") {
        return <span className="orderBy asc"><FontAwesomeIcon icon={faCaretSquareUp}/></span>;
      } else {
        return <span className="orderBy desc"><FontAwesomeIcon icon={faCaretSquareDown}/></span>;
      }
    } else {
      return <span className="orderBy"></span>;
    }
  }

  sort = (colName) => {
    if (colName === this.state.pageControl.orderByCol) {
      const sortOrder = this.state.pageControl.orderBySort === "ASC" ? "DESC" : "ASC"
      this.renderAgentRecords(
        this.state.pageControl.currentPage, colName + " " + sortOrder
      );
    } else {
      this.renderAgentRecords(
        this.state.pageControl.currentPage, colName + " ASC"
      );
    }
  }

  render() {
    const globalError = this.state.globalError === '' ? "" :
          <article className="message is-danger">
            <div className="message-body">{this.state.globalError}</div>
          </article>;

    const table = this.state.records.length === 0 ?
          <span className="emptyMessage">{this.msg('recordEmpty')}</span>
          :
          <table className="table score">
            <thead>
              <tr>
                <th className="rank">{this.msg('rank')}</th>
                <th className={cx("agentName sortable", {"ordered": this.state.pageControl.orderByCol === "agent_name"})}
                    onClick={() => this.sort('agent_name')}>
                  {this.renderOrderByChar('agent_name')}{this.msg('abbrevAgentName')}
                </th>
                <th className="faction">{this.msg('abbrevFaction')}</th>
                <th className="startLevel">{this.msg('abbrevStartLevel')}</th>
                <th className="endLevel">{this.msg('abbrevEndLevel')}</th>
                <th className="earnedLevel">{this.msg('abbrevEarnedLevel')}</th>
                <th className="startAp">{this.msg('abbrevStartAp')}</th>
                <th className="endAp">{this.msg('abbrevEndAp')}</th>
                <th className={cx("earnedAp sortable", {"ordered": this.state.pageControl.orderByCol === "lifetime_ap_earned"})}
                    onClick={() => this.sort('lifetime_ap_earned')}>
                  {this.renderOrderByNum('lifetime_ap_earned')}{this.msg('abbrevEarnedAp')}
                </th>
                <th className="startWalked">{this.msg('abbrevStartWalked')}</th>
                <th className="endWalked">{this.msg('abbrevEndWalked')}</th>
                <th className={cx("earnedWalked sortable", {"ordered": this.state.pageControl.orderByCol === "distance_walked_earned"})}
                    onClick={() => this.sort('distance_walked_earned')}>
                  {this.renderOrderByNum('distance_walked_earned')}{this.msg('abbrevEarnedWalked')}
                </th>
                <th className="updatedTime">{this.msg('abbrevUpdatedTime')}</th>
              </tr>
            </thead>
            
            <tbody>
              {
                this.state.records.map((e) =>
                  <tr key={e.agentName}>
                    <td className="rank">{e.rank + 1}</td>
                    <td className="agentName">{e.agentName}</td>
                    <td className="faction">{e.faction}</td>
                    <td className="startLevel">{e.startLevel}</td>
                    <td className="endLevel">{e.endLevel}</td>
                    <td className="earnedLevel">{e.earnedLevel}</td>
                    <td className="startAp">{Number(e.startAp).toLocaleString()}</td>
                    <td className="endAp">{Number(e.endAp).toLocaleString()}</td>
                    <td className="earnedAp">{Number(e.earnedAp).toLocaleString()}</td>
                    <td className="startWalked">{Number(e.startWalked).toLocaleString()}</td>
                    <td className="endWalked">{Number(e.endWalked).toLocaleString()}</td>
                    <td className="earnedWalked">{Number(e.earnedWalked).toLocaleString()}</td>
                    <td className="updatedTime">{e.createdAt}</td>
                  </tr>
                )
              }
            </tbody>
          </table>;

    const paginator = this.state.pagination === undefined ? "" :
          <nav className="pagination" role="navigation" aria-label="pagination">
            <a className="pagination-previous pagingButton" disabled={!this.state.pageControl.prevPageExists}
               onClick={this.prevPage} href="#prevPage">
              <i className="fas fa-chevron-circle-left fa-2x"></i>
            </a>
            <a className="pagination-next pagingButton" disabled={!this.state.pageControl.nextPageExists}
               onClick={this.nextPage} href="#nextPage">
              <i className="fas fa-chevron-circle-right fa-2x"></i>
            </a>
            <ul className="pagination-list">
              {this.state.pagination.topButtonExists ? (
                <li>
                  <a className="pagination-link" aria-label="Goto top page" href="#gotoTopPage">1</a>
                </li>
              ) : ""}
              {this.state.pagination.topButtonExists ? (
                <li>
                  <span className="pagination-ellipsis">&hellip;</span>
                </li>
              ) : ""}
              {(() => {
                const pages = [];
                for (let i = 0; i < this.state.pagination.showPageCount; ++i) {
                  const p = this.state.pagination.startPage + i + 1;
                  pages.push(
                    <li key={i}>
                      <a className={cx("pagination-link",{ "is-current": this.state.pageControl.currentPage + 1 === p})}
                         aria-label={"Goto page " + p} onClick={() => this.gotoPage(p - 1)} href="#gotoPage">{p}</a>
                    </li>
                  );
                }
                return pages;
              })()}
              {this.state.pagination.lastButtonExists ? (
                <li>
                  <span className="pagination-ellipsis">&hellip;</span>
                </li>
              ): ""}
              {this.state.pagination.lastButtonExists ? (
                <li>
                  <a className="pagination-link" aria-label="Goto last page" href="#gotoLastPage">
                    {this.state.pageControl.pageCount - 1}
                  </a>
                </li>
              ): ""}
            </ul>
          </nav>;

    return (
      <div className="attend">
        {globalError}

        <nav className="panel">
          <p className="panel-heading">
            {this.msg('scoreList')}
          </p>

          <div className="panel-block">
            <div className="siteNameWrapper">
              <span className="siteName">{this.state.siteName}</span>
              <a href="#registerAgentRecord" className="button is-info" onClick={this.registerMyRecord}>
                {this.msg('registerMyRecord')}
              </a>
            </div>
          </div>

          <div className="panel-block">
            <div className="recordsWrapper">
              <div className="tableWrapper">
                {table}
              </div>
              <div className="paginatorWrapper">{paginator}</div>
            </div>
          </div>
        </nav>
      </div>
    );
  }
}

export default withRouter(AgentRecords);
